package app

import domain.repos.ChatsRepository
import domain.repos.MessagesRepository
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
import domain.entities.ClientAction
import org.example.domain.entities.ServerAction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Application: KoinComponent {

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

                    var userId: Int? = null
                    val backFlow = MutableSharedFlow<ServerAction>()

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val receivedText = frame.readText()
                        try {
                            val clientAction = Json.decodeFromString<ClientAction>(receivedText)
                            if (clientAction is ClientAction.Authorize) {
                                userId = getUserIdFromJWT(clientAction.jwt)
                            }
                            else {
                                handleClientAction(clientAction, userId ?: continue, backFlow)
                            }
                        }
                        catch (_: Exception) {

                        }
                    }

                    backFlow.collect {
                        send(
                            Json.encodeToString(it)
                        )
                    }
                }
            }
        }
    }

    private fun handleClientAction(action: ClientAction, userId: Int, backFlow: MutableSharedFlow<ServerAction>) {
        when (action) {
            is ClientAction.SendMessage -> {
                scope.launch {
                    val message = messagesRepo.createMessage(
                        chatId = action.chatId,
                        senderId = userId,
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
        }
    }

    private fun getUserIdFromJWT(jwt: String): Int {
        return jwt.toInt()
    }
}