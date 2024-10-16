package app

import entities.ClientAction
import entities.ServerAction
import entities.User
import entities.UserType
import shared_domain.repos.ChatsRepository
import shared_domain.repos.MessagesRepository
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Application: KoinComponent {

    private val globalBackFlow = MutableSharedFlow<ServerAction>()

    private val chatsRepo: ChatsRepository by inject()
    private val messagesRepo: MessagesRepository by inject()

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    private val scope = CoroutineScope(Dispatchers.Default)

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
                webSocket("/messenger") {

                    var user: User? = null
                    val backFlow = MutableSharedFlow<ServerAction>()

                    scope.launch {
                        backFlow.collect {
                            Json.encodeToString(it).let {
                                send(it)
                                println("Sent: $it")
                            }
                        }
                    }
                    scope.launch {
                        globalBackFlow.collect {
                            Json.encodeToString(it).let {
                                send(it)
                                println("Sent: $it")
                            }
                        }
                    }

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val receivedText = frame.readText()
                        println("Received: $receivedText")
                        try {
                            val clientAction = Json.decodeFromString<ClientAction>(receivedText)
                            if (clientAction is ClientAction.Authorize) {
                                user = getUserFromJWT(clientAction.jwt)
                            }
                            else {
                                handleClientAction(clientAction, user ?: continue, globalBackFlow)
                            }
                        }
                        catch (e: Exception) {
                            println(e.message.toString())
                        }
                    }
                }
            }
        }
    }

    private fun handleClientAction(action: ClientAction, user: User, backFlow: MutableSharedFlow<ServerAction>) {
        when (action) {


            is ClientAction.SendMessage -> {
                scope.launch {
                    val message = messagesRepo.createMessage(
                        chatId = action.chatId,
                        senderId = user.id,
                        text = action.message
                    )

                    backFlow.emit(ServerAction.NewMessage(
                        chatId = action.chatId,
                        message = message
                    ))
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
                        ServerAction.NewChat(
                            chat = chat
                        )
                    )
                }
            }

            is ClientAction.RequestChatMessages -> {
                scope.launch {
                    val messages = messagesRepo.getChatMessages(
                        chatId = action.chatId
                    )
                    backFlow.emit(
                        ServerAction.SendChatMessages(
                            messages = messages,
                            chatId = action.chatId
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
                    )

                    backFlow.emit(
                        ServerAction.SendChatsList(
                            chats = chats
                        )
                    )
                }
            }
        }
    }

    private fun getUserFromJWT(jwt: String): User {
        return User(
            id = jwt.toInt(),
            type = UserType.DEFAULT
        )
    }
}