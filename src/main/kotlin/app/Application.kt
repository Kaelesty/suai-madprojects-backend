package app

import domain.IntegrationService
import domain.UnreadMessagesRepository
import entities.*
import entities.ServerAction.*
import shared_domain.repos.ChatsRepository
import shared_domain.repos.MessagesRepository
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Application: KoinComponent {

    class Session(
        val user: User,
        val projectId: Int,
        val projectMembers: List<User>,
    )

    private val projectBackFlows: MutableMap<Int, MutableSharedFlow<ServerAction>> = mutableMapOf()

    private val chatsRepo: ChatsRepository by inject()
    private val messagesRepo: MessagesRepository by inject()
    private val integrationRepo: IntegrationService by inject()
    private val unreadMessagesRepo: UnreadMessagesRepository by inject()

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

                routing {
                    singlePageApplication {
                        useResources = true
                        filesPath = "react-app"
                        defaultPage = "index.html"
                        applicationRoute = "/app"
                    }
                }

                webSocket("/messenger") {


                    val localScope = CoroutineScope(Dispatchers.IO)
                    var session: Session? = null
                    var backFlow: ProjectBackFlowManager.ProjectBackFlow.BackFlow? = null
                    val localBackFlow = MutableSharedFlow<ServerAction>()

                    localScope.launch {
                        localBackFlow.collect {
                            send(Frame.Text(
                                Json.encodeToString(it)
                            ))
                        }
                    }

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val receivedText = frame.readText()
                        println("Received: $receivedText")
                        try {
                            val clientAction = Json.decodeFromString<ClientAction>(receivedText)
                            if (clientAction is ClientAction.Authorize) {
                                session = Session(
                                    user = integrationRepo.getUserFromJWT(clientAction.jwt),
                                    projectId = clientAction.projectId,
                                    projectMembers = integrationRepo.getProjectUsers(clientAction.projectId)
                                )
                                backFlow = ProjectBackFlowManager.getProjectBackFlow(
                                    projectId = clientAction.projectId,
                                ).apply {
                                    collect {
                                        send(
                                            Frame.Text(
                                                Json.encodeToString(it)
                                            )
                                        )
                                    }
                                }
                            }
                            else if (clientAction is ClientAction.CloseSession) {
                                close()
                            }
                            else {
                                handleClientAction(
                                    localScope,
                                    clientAction,
                                    session?.user ?: continue,
                                    backFlow ?: continue,
                                    localBackFlow
                                )
                            }
                        }
                        catch (e: Exception) {
                            println(e.message.toString())
                        }
                    }
                    localScope.cancel()
                    session?.projectId?.let {
                        ProjectBackFlowManager.unsubscribe(it)
                    }
                }
            }
        }
    }

    private fun handleClientAction(
        scope: CoroutineScope,
        action: ClientAction,
        user: User,
        backFlow: ProjectBackFlowManager.ProjectBackFlow.BackFlow,
        localBackFlow: MutableSharedFlow<ServerAction>
    ) {
        when (action) {



            is ClientAction.SendMessage -> {
                scope.launch {
                    val message = messagesRepo.createMessage(
                        chatId = action.chatId,
                        senderId = user.id,
                        text = action.message
                    )

                    unreadMessagesRepo.readMessagesBefore(
                        messageId = message.id,
                        chatId = action.chatId,
                        userId = user.id
                    )

                    backFlow.emit(
                        NewMessage(
                            chatId = action.chatId,
                            message = message
                        )
                    )
                }
            }

            is ClientAction.CreateChat -> {
                scope.launch {
                    val chat = chatsRepo.createChat(
                        title = action.chatTitle,
                        projectId = action.projectId,
                        chatType = action.chatType
                    )

                    backFlow.emit(
                        NewChat(
                            chat = chat
                        )
                    )
                }
            }

            is ClientAction.RequestChatMessages -> {
                scope.launch {
                    val unreadMessagesIds = messagesRepo.getUnreadMessagesId(
                        chatId = action.chatId,
                        userId = user.id
                    )
                    val readMessages = mutableListOf<Message>()
                    val unreadMessages = mutableListOf<Message>()
                    messagesRepo.getChatMessages(
                        chatId = action.chatId
                    ).forEach {
                        if (it.id in unreadMessagesIds) {
                            unreadMessages.add(it)
                        } else readMessages.add(it)
                    }
                    localBackFlow.emit(
                        SendChatMessages(
                            chatId = action.chatId,
                            readMessages = readMessages,
                            unreadMessages = unreadMessages,
                        )
                    )
                }
            }

            is ClientAction.Authorize -> { /* handled before method call */ }

            is ClientAction.RequestChatsList -> {
                scope.launch {

                    val chats = chatsRepo.getProjectChats(
                        projectId = action.projectId,
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

            is ClientAction.CloseSession -> { /* handled before method call */ }

            is ClientAction.ReadMessage -> {
                scope.launch {
                    messagesRepo.readMessage(
                        messageId = action.messageId,
                        userId = user.id
                    )
                    localBackFlow.emit(
                        MessageReadRecorded(
                            messageId = action.messageId,
                            chatId = action.chatId
                        )
                    )
                }
            }

            is ClientAction.ReadMessagesBefore -> {
                scope.launch {
                    unreadMessagesRepo.readMessagesBefore(
                        messageId = action.messageId,
                        chatId = action.chatId,
                        userId = user.id
                    )
                }
            }
        }
    }
}