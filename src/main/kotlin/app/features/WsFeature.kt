package app.features

import app.ProjectBackFlowManager
import com.auth0.jwt.JWTVerifier
import data.schemas.ProjectCuratorshipService
import data.schemas.ProjectMembershipService
import domain.GithubTokensRepo
import domain.KanbanRepository
import domain.UnreadMessagesRepository
import domain.activity.ActivityRepo
import domain.activity.ActivityType
import domain.profile.ProfileRepo
import entities.Action
import entities.Action.Kanban.SetState
import entities.Action.Messenger.MessageReadRecorded
import entities.Action.Messenger.NewChat
import entities.Action.Messenger.NewMessage
import entities.Action.Messenger.SendChatMessages
import entities.Action.Messenger.SendChatsList
import entities.Action.Messenger.UpdateChatUnreadCount
import entities.ChatSender
import entities.Intent
import entities.Message
import entities.User
import entities.UserType
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import shared_domain.repos.ChatsRepository
import shared_domain.repos.MessagesRepository

interface WsFeature {

    suspend fun install(serverSession: DefaultWebSocketServerSession)
}

class WsFeatureImpl(
    private val kanbanRepository: KanbanRepository,
    private val messagesRepo: MessagesRepository,
    private val unreadMessagesRepo: UnreadMessagesRepository,
    private val chatsRepo: ChatsRepository,
    private val jwt: JWTVerifier,
    private val profileRepo: ProfileRepo,
    private val projectMembershipService: ProjectMembershipService,
    private val projectCuratorshipService: ProjectCuratorshipService,
    // TODO REMOVE SERVICES
    private val githubTokensRepo: GithubTokensRepo,
    private val activityRepo: ActivityRepo,
) : WsFeature {

    override suspend fun install(serverSession: DefaultWebSocketServerSession) {
        with(serverSession) {
            val localScope = CoroutineScope(Dispatchers.IO)
            var session: MutableStateFlow<Context?> = MutableStateFlow(null)
            val localBackFlow = MutableSharedFlow<ActionHolder>()

            localScope.launch {
                localBackFlow.collect {
                    val currentSession = session.value
                    val sendToMessengerFlag = it.action is Action.Messenger
                            && currentSession?.projectSessions
                        ?.first { project -> project.id == it.projectId }
                        ?.observeMessenger == true
                    val sendToKanbanFlag = it.action is Action.Kanban
                            && currentSession?.projectSessions
                        ?.first { project -> project.id == it.projectId }
                        ?.observeKanban == true
                    if (sendToKanbanFlag || sendToMessengerFlag || it.action is Action.Unauthorized) {
                        send(
                            Frame.Text(
                                Json.encodeToString(it.action).also {
                                    println("Sent: $it")
                                }
                            )
                        )
                    }
                }
            }

            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val receivedText = frame.readText()
                println("Received: $receivedText")
                try {

                    val projectId = getProjectId(receivedText)
                    val intent = Json.decodeFromString<Intent>(receivedText)

                    if (intent !is Intent.Authorize && session.value == null) {
                        localBackFlow.emit(
                            ActionHolder(
                                Action.Unauthorized, -1
                            )
                        )
                    }

                    when (intent) {
                        is Intent.Authorize -> {
                            val userId = jwt.verify(intent.jwt).getClaim("userId").asString()
                            session.emit(
                                Context(
                                    user = User(
                                        id = userId,
                                        type = if (!profileRepo.checkIsCurator(userId)) UserType.COMMON else UserType.CURATOR
                                    ),
                                    projectSessions = listOf()
                                )
                            )
                        }

                        Intent.CloseSession -> close()
                        is Intent.Kanban.Start -> {
                            session.value?.projectSessions?.let {
                                val newSessions = if (it.any { it.id == intent.projectId }) {
                                    it.map {
                                        if (it.id == intent.projectId) {
                                            it.copy(
                                                observeKanban = true
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                } else {
                                    it.toMutableList().apply {
                                        val backflow: ProjectBackFlowManager.ProjectBackFlow.BackFlow =
                                            getBackFlow(intent.projectId, session)
                                        add(
                                            ProjectSession(
                                                id = intent.projectId,
                                                projectBackFlow = backflow,
                                                observeKanban = true,
                                                observeMessenger = false
                                            )
                                        )
                                    }
                                }
                                session.emit(
                                    session.value?.copy(projectSessions = newSessions)
                                )
                            }
                        }

                        is Intent.Kanban.Stop -> session.value?.let {
                            session.emit(
                                it.copy(
                                    projectSessions = it.projectSessions.map {
                                        if (it.id == intent.projectId) {
                                            it.copy(
                                                observeKanban = false
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                )
                            )
                        }

                        is Intent.Messenger.Start -> {
                            session.value?.projectSessions?.let {
                                val newSessions = if (it.any { it.id == intent.projectId }) {
                                    it.map {
                                        if (it.id == intent.projectId) {
                                            it.copy(
                                                observeMessenger = true
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                } else {
                                    it.toMutableList().apply {
                                        val backflow: ProjectBackFlowManager.ProjectBackFlow.BackFlow =
                                            getBackFlow(intent.projectId, session)
                                        add(
                                            ProjectSession(
                                                id = intent.projectId,
                                                projectBackFlow = backflow,
                                                observeKanban = false,
                                                observeMessenger = true
                                            )
                                        )
                                    }
                                }
                                session.emit(
                                    session.value?.copy(
                                        projectSessions = newSessions
                                    )
                                )
                            }
                        }

                        is Intent.Messenger.Stop -> session.value?.let {
                            session.emit(
                                it.copy(
                                    projectSessions = it.projectSessions.map {
                                        if (it.id == intent.projectId) {
                                            it.copy(
                                                observeMessenger = false
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                )
                            )
                        }

                        else -> {
                            val sessionValue = session.value
                            handleIntent(
                                localScope,
                                intent,
                                sessionValue?.user ?: continue,
                                sessionValue.projectSessions.first {
                                    it.id == projectId
                                },
                                localBackFlow,
                            )
                        }
                    }
                } catch (e: Exception) {
                    println(e.toString())
                }
            }
            localScope.cancel()
            session.value?.projectSessions?.let {
                it.forEach {
                    ProjectBackFlowManager.unsubscribe(it.id)
                }
            }
        }
    }

    private fun getProjectId(text: String): Int {
        if (text.contains("entities.Intent.Authorize")) return -1
        val part = text.split("projectId")
        var num = ""
        var addFlag = true
        part[1].forEachIndexed { index, it ->
            if (index != 0 && index != 1 && addFlag) {
                if (it in "1234567890") {
                    num += it
                } else addFlag = false
            }
        }
        if (num == "") {
            throw Exception()
        }
        return num.toInt()
    }

    private fun handleIntent(
        scope: CoroutineScope,
        intent: Intent,
        user: User,
        session: ProjectSession,
        localBackFlow: MutableSharedFlow<ActionHolder>
    ) {
        when (intent) {
            is Intent.Messenger -> {
                handleMessengerIntent(
                    scope, intent, user, session, localBackFlow
                )
            }

            is Intent.Kanban -> {
                handleKanbanIntent(
                    scope, intent, user, session
                )
            }

            is Intent.Authorize -> { /* handled before */
            }

            Intent.CloseSession -> { /* handled before */
            }
        }
    }

    private fun handleKanbanIntent(
        scope: CoroutineScope,
        intent: Intent.Kanban,
        user: User,
        session: ProjectSession,
    ) {

        fun run(block: suspend () -> Unit) {
            scope.launch {
                block()
                session.projectBackFlow.emit(
                    SetState(
                        kanbanRepository.getKanban(session.id),
                        projectId = session.id
                    ),
                    projectId = session.id
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

                    activityRepo.recordActivity(
                        projectId = session.id.toString(),
                        actorId = user.id,
                        targetTitle = kanbanRepository.getKardTitle(intent.id),
                        targetId = intent.id.toString(),
                        type = ActivityType.KardMove,
                        secondaryTargetTitle = kanbanRepository.getColumnTitle(intent.columnId)
                    )
                }
            }

            is Intent.Kanban.GetKanban -> {
                scope.launch {
                    val kanban = kanbanRepository.getKanban(session.id)
                    session.projectBackFlow.emit(
                        SetState(kanban, session.id),
                        session.id
                    )
                }
            }

            is Intent.Kanban.Start -> { /* handled before */
            }

            is Intent.Kanban.Stop -> { /* handled before */
            }

            is Intent.Kanban.CreateColumn -> {
                run {
                    kanbanRepository.createColumn(
                        projectId = session.id,
                        name = intent.name
                    )
                }
            }

            is Intent.Kanban.MoveColumn -> {
                run {
                    kanbanRepository.moveColumn(
                        projectId = session.id,
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
        session: ProjectSession,
        localBackFlow: MutableSharedFlow<ActionHolder>
    ) {
        when (intent) {
            is Intent.Messenger.SendMessage -> {
                scope.launch {
                    session
                    val message = messagesRepo.createMessage(
                        chatId = intent.chatId,
                        senderId = user.id,
                        text = intent.message,
                        projectId = session.id
                    )

                    unreadMessagesRepo.readMessagesBefore(
                        messageId = message.id,
                        chatId = intent.chatId,
                        userId = user.id
                    )

                    session.projectBackFlow.emit(
                        NewMessage(
                            chatId = intent.chatId,
                            message = message,
                            projectId = session.id
                        ),
                        session.id
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

                    session.projectBackFlow.emit(
                        NewChat(
                            chat = chat,
                            session.id
                        ),
                        projectId = session.id
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
                        chatId = intent.chatId,
                        userType = user.type
                    ).forEach {
                        if (it.id in unreadMessagesIds) {
                            unreadMessages.add(it)
                        } else readMessages.add(it)
                    }
                    localBackFlow.emit(
                        ActionHolder(
                            projectId = session.id,
                            action = SendChatMessages(
                                chatId = intent.chatId,
                                readMessages = readMessages,
                                unreadMessages = unreadMessages,
                                projectId = session.id
                            )
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

                    val members = projectMembershipService.getProjectUserIds(session.id.toString()).map {
                        profileRepo.getCommonById(it.toString()).let {
                            if (it == null) {
                                null
                            }
                            else {
                                val githubMeta = githubTokensRepo.getUserMeta(it.data.id)
                                ChatSender(
                                    id = it.data.id,
                                    firstName = it.data.firstName,
                                    secondName = it.data.secondName,
                                    lastName = it.data.lastName,
                                    avatar = githubMeta?.githubAvatar
                                )
                            }
                        }
                    }.filterNotNull()
                    val curators = projectCuratorshipService.getProjectCurator(session.id).map {
                        profileRepo.getCuratorById(it.toString()).let {
                            if (it == null) {
                                null
                            }
                            else {
                                val githubMeta = githubTokensRepo.getUserMeta(it.data.id)
                                ChatSender(
                                    id = it.data.id,
                                    firstName = it.data.firstName,
                                    secondName = it.data.secondName,
                                    lastName = it.data.lastName,
                                    avatar = githubMeta?.githubAvatar
                                )
                            }
                        }
                    }.filterNotNull()

                    localBackFlow.emit(
                        ActionHolder(
                            projectId = session.id,
                            action = SendChatsList(
                                chats = chats,
                                projectId = session.id,
                                senders = members + curators
                            )
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
                        ActionHolder(
                            projectId = session.id,
                            action = MessageReadRecorded(
                                messageId = intent.messageId,
                                chatId = intent.chatId,
                                projectId = session.id
                            )
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
                        ActionHolder(
                            projectId = session.id,
                            action = UpdateChatUnreadCount(
                                chatId = intent.chatId,
                                count = unreadCount,
                                projectId = session.id
                            )
                        )
                    )
                }
            }

            is Intent.Messenger.Start -> { /* handled before */
            }

            is Intent.Messenger.Stop -> { /* handled before */
            }
        }
    }

    private fun DefaultWebSocketServerSession.getBackFlow(
        projectId: Int,
        session: MutableStateFlow<Context?>
    ): ProjectBackFlowManager.ProjectBackFlow.BackFlow {
        val backflow: ProjectBackFlowManager.ProjectBackFlow.BackFlow = ProjectBackFlowManager.getProjectBackFlow(
            projectId = projectId,
        ).apply {
            launch {
                while (session.value != null) {
                    // react-front drop socket-connection after some period of inaction
                    // this is required to keep it alive
                    send(
                        Frame.Text(
                            Json.encodeToString(Action.KeepAlive)
                        )
                    )
                    delay(10_000)
                }
            }
            collect {
                val currentSession = session.value
                val sendToMessengerFlag = it.action is Action.Messenger
                        && currentSession?.projectSessions
                    ?.first { project -> project.id == it.projectId }
                    ?.observeMessenger == true
                val sendToKanbanFlag = it.action is Action.Kanban
                        && currentSession?.projectSessions
                    ?.first { project -> project.id == it.projectId }
                    ?.observeKanban == true
                if (sendToKanbanFlag || sendToMessengerFlag) {
                    send(
                        Frame.Text(
                            Json.encodeToString(it.action).also {
                                println("Sent: $it")
                            }
                        )
                    )
                }
            }
        }
        return backflow
    }
}

data class Context(
    val user: User,
    val projectSessions: List<ProjectSession>
)

data class ProjectSession(
    val id: Int,
    val projectBackFlow: ProjectBackFlowManager.ProjectBackFlow.BackFlow,
    val observeKanban: Boolean,
    val observeMessenger: Boolean,
)

class ActionHolder(
    val action: Action,
    val projectId: Int,
)