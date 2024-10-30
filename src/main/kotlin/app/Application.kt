package app

import domain.IntegrationService
import domain.KanbanRepository
import domain.UnreadMessagesRepository
import entities.*
import entities.Action.Kanban.*
import entities.Action.Messenger.*
import shared_domain.repos.ChatsRepository
import shared_domain.repos.MessagesRepository
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Application: KoinComponent {

    data class Session(
        val user: User,
        val projectId: Int,
        val observeMessenger: Boolean,
        val observeKanban: Boolean,
    )

    private val projectBackFlows: MutableMap<Int, MutableSharedFlow<Action>> = mutableMapOf()

    private val chatsRepo: ChatsRepository by inject()
    private val messagesRepo: MessagesRepository by inject()
    private val integrationRepo: IntegrationService by inject()
    private val unreadMessagesRepo: UnreadMessagesRepository by inject()
    private val kanbanRepository: KanbanRepository by inject()

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    init {
        setup()
    }

    fun run() {
        server.start(wait = true)
    }

    private fun setup() {
        server = embeddedServer(Netty, port = 8080) {
            install(WebSockets)
            routing {

                webSocket("/project") {


                    val localScope = CoroutineScope(Dispatchers.IO)
                    var session: MutableStateFlow<Session?> = MutableStateFlow(null)
                    var backFlow: ProjectBackFlowManager.ProjectBackFlow.BackFlow? = null
                    val localBackFlow = MutableSharedFlow<Action>()

                    localScope.launch {
                        localBackFlow.collect {
                            val currentSession = session.value
                            val sendToMessengerFlag = it is Action.Messenger
                                    && currentSession?.observeMessenger == true
                            val sendToKanbanFlag = it is Action.Kanban
                                    && currentSession?.observeKanban == true
                            if (sendToKanbanFlag || sendToMessengerFlag) {
                                send(Frame.Text(
                                    Json.encodeToString(it)
                                ))
                            }
                        }
                    }

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val receivedText = frame.readText()
                        println("Received: $receivedText")
                        try {
                            val intent = Json.decodeFromString<Intent>(receivedText)

                            when (intent) {
                                is Intent.Authorize -> {
                                    backFlow = authorizeAndGetBackflow(session, intent, backFlow)
                                }
                                Intent.CloseSession -> close()
                                Intent.Kanban.Start -> {
                                    session.update {
                                        it?.copy(observeKanban = true)
                                    }
                                }
                                Intent.Kanban.Stop -> session.update {
                                    it?.copy(observeKanban = false)
                                }
                                Intent.Messenger.Start -> session.update {
                                    it?.copy(observeMessenger = true)
                                }
                                Intent.Messenger.Stop -> session.update {
                                    it?.copy(observeMessenger = false)
                                }
                                else -> {
                                    handleIntent(
                                        localScope,
                                        intent,
                                        session.value?.user ?: continue,
                                        session.value?.projectId ?: continue,
                                        backFlow ?: continue,
                                        localBackFlow,
                                    )
                                }
                            }
                        }
                        catch (e: Exception) {
                            println(e.message.toString())
                        }
                    }
                    localScope.cancel()
                    session.value?.projectId?.let {
                        ProjectBackFlowManager.unsubscribe(it)
                    }
                }
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.authorizeAndGetBackflow(
        session: MutableStateFlow<Session?>,
        intent: Intent.Authorize,
        localBackflow: ProjectBackFlowManager.ProjectBackFlow.BackFlow?
    ): ProjectBackFlowManager.ProjectBackFlow.BackFlow? {
        var backflow = localBackflow
        session.emit(
            Session(
                user = integrationRepo.getUserFromJWT(intent.jwt),
                projectId = intent.projectId,
                observeMessenger = false,
                observeKanban = false,
            )
        )
        backflow = ProjectBackFlowManager.getProjectBackFlow(
            projectId = intent.projectId,
        ).apply {
            launch {
                while (session.value != null) {
                    // react-front drop socket-connection after some period of inaction
                    // this is required to keep it alive
                    send(Frame.Text(
                        Json.encodeToString(Action.KeepAlive)
                    ))
                    delay(10_000)
                }
            }
            collect {
                val currentSession = session.value
                val sendToMessengerFlag = it is Action.Messenger
                        && currentSession?.observeMessenger == true
                val sendToKanbanFlag = it is Action.Kanban
                        && currentSession?.observeKanban == true
                if (sendToKanbanFlag || sendToMessengerFlag) {
                    send(Frame.Text(
                        Json.encodeToString(it)
                    ))
                }
            }
        }
        return backflow
    }

    private fun handleIntent(
        scope: CoroutineScope,
        intent: Intent,
        user: User,
        projectId: Int,
        backFlow: ProjectBackFlowManager.ProjectBackFlow.BackFlow,
        localBackFlow: MutableSharedFlow<Action>
    ) {
        when (intent) {
            is Intent.Messenger -> { handleMessengerIntent(
                scope, intent, user, backFlow, localBackFlow
            ) }
            is Intent.Kanban -> { handleKanbanIntent(
                scope, intent, user, projectId, backFlow, localBackFlow
            ) }
            is Intent.Authorize -> { /* handled before */ }
            Intent.CloseSession -> { /* handled before */ }
        }
    }

    private fun handleKanbanIntent(
        scope: CoroutineScope,
        intent: Intent.Kanban,
        user: User,
        projectId: Int,
        backFlow: ProjectBackFlowManager.ProjectBackFlow.BackFlow,
        localBackFlow: MutableSharedFlow<Action>
    ) {

        fun run(block: suspend () -> Unit) {
            scope.launch {
                block()
                backFlow.emit(
                    Action.Kanban.SetState(kanbanRepository.getKanban(projectId))
                )
            }
        }

        when (intent) {

            is Intent.Kanban.CreateKard -> {
                run {
                    kanbanRepository.createKard(
                        name = intent.name,
                        columnId = intent.columnId,
                        desc = intent.desc,
                        authorId = user.id
                    )
                }
            }
            is Intent.Kanban.MoveKard -> {
                run {
                    kanbanRepository.moveKard(
                        columnId = intent.columnId,
                        kardId = intent.id,
                        newOrder = intent.newPosition,
                        newColumnId = intent.newColumnId
                    )
                }
            }
            is Intent.Kanban.GetKanban -> {
                scope.launch {
                    val kanban = kanbanRepository.getKanban(projectId)
                    backFlow.emit(
                        SetState(kanban)
                    )
                }
            }
            Intent.Kanban.Start -> { /* handled before */ }
            Intent.Kanban.Stop -> { /* handled before */ }
            is Intent.Kanban.CreateColumn -> {
                run {
                    kanbanRepository.createColumn(
                        projectId = projectId,
                        name = intent.name
                    )
                }
            }
            is Intent.Kanban.MoveColumn -> {
                run {
                    kanbanRepository.moveColumn(
                        projectId = projectId,
                        columnId = intent.id,
                        newOrder = intent.newPosition
                    )
                }
            }

            is Intent.Kanban.UpdateKard -> {
                run {
                    kanbanRepository.updateKard(
                        id = intent.id,
                        desc = intent.desc,
                        name = intent.name,
                    )
                }
            }

            is Intent.Kanban.UpdateColumn -> {
                run {
                    kanbanRepository.updateColumn(intent.id, intent.name)
                }
            }

            is Intent.Kanban.DeleteColumn -> {
                run {
                    kanbanRepository.deleteColumn(intent.id)
                }
            }
            is Intent.Kanban.DeleteKard -> {
                run {
                    kanbanRepository.deleteKard(intent.id)
                }
            }
        }
    }

    private fun handleMessengerIntent(
        scope: CoroutineScope,
        intent: Intent.Messenger,
        user: User,
        backFlow: ProjectBackFlowManager.ProjectBackFlow.BackFlow,
        localBackFlow: MutableSharedFlow<Action>
    ) {
        when (intent) {
            is Intent.Messenger.SendMessage -> {
                scope.launch {
                    val message = messagesRepo.createMessage(
                        chatId = intent.chatId,
                        senderId = user.id,
                        text = intent.message
                    )

                    unreadMessagesRepo.readMessagesBefore(
                        messageId = message.id,
                        chatId = intent.chatId,
                        userId = user.id
                    )

                    backFlow.emit(
                        NewMessage(
                            chatId = intent.chatId,
                            message = message
                        )
                    )
                }
            }

            is Intent.Messenger.CreateChat -> {
                scope.launch {
                    val chat = chatsRepo.createChat(
                        title = intent.chatTitle,
                        projectId = intent.projectId,
                        chatType = intent.chatType
                    )

                    backFlow.emit(
                        NewChat(
                            chat = chat
                        )
                    )
                }
            }

            is Intent.Messenger.RequestChatMessages -> {
                scope.launch {
                    val unreadMessagesIds = messagesRepo.getUnreadMessagesId(
                        chatId = intent.chatId,
                        userId = user.id
                    )
                    val readMessages = mutableListOf<Message>()
                    val unreadMessages = mutableListOf<Message>()
                    messagesRepo.getChatMessages(
                        chatId = intent.chatId
                    ).forEach {
                        if (it.id in unreadMessagesIds) {
                            unreadMessages.add(it)
                        } else readMessages.add(it)
                    }
                    localBackFlow.emit(
                        SendChatMessages(
                            chatId = intent.chatId,
                            readMessages = readMessages,
                            unreadMessages = unreadMessages,
                        )
                    )
                }
            }

            is Intent.Messenger.RequestChatsList -> {
                scope.launch {

                    val chats = chatsRepo.getProjectChats(
                        projectId = intent.projectId,
                        userType = user.type
                    ).map {

                        it.copy(
                            unreadMessagesCount = unreadMessagesRepo.getUnreadMessagesCount(
                                userId = user.id,
                                chatId = it.id
                            ),
                            lastMessage = messagesRepo.getLastMessage(chatId = it.id),
                        )
                    }

                    localBackFlow.emit(
                        SendChatsList(
                            chats = chats
                        )
                    )
                }
            }

            is Intent.Messenger.ReadMessage -> {
                scope.launch {
                    messagesRepo.readMessage(
                        messageId = intent.messageId,
                        userId = user.id
                    )
                    localBackFlow.emit(
                        MessageReadRecorded(
                            messageId = intent.messageId,
                            chatId = intent.chatId
                        )
                    )
                }
            }

            is Intent.Messenger.ReadMessagesBefore -> {
                scope.launch {
                    unreadMessagesRepo.readMessagesBefore(
                        messageId = intent.messageId,
                        chatId = intent.chatId,
                        userId = user.id
                    )
                    val unreadCount = unreadMessagesRepo.getUnreadMessagesCount(
                        userId = user.id,
                        chatId = intent.chatId
                    )
                    localBackFlow.emit(
                        UpdateChatUnreadCount(
                            chatId = intent.chatId,
                            count = unreadCount
                        )
                    )
                }
            }
            Intent.Messenger.Start -> { /* handled before */ }
            Intent.Messenger.Stop -> { /* handled before */ }
        }
    }
}