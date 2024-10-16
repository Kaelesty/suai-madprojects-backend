package domain.entities

import kotlinx.serialization.Serializable
import org.example.domain.entities.ChatType

@Serializable
sealed interface ClientAction {

    @Serializable
    class Authorize(
        val jwt: String
    ): ClientAction

    @Serializable
    class SendMessage(
        val chatId: Int,
        val message: String
    ): ClientAction

    @Serializable
    class CreateChat(
        val projectId: Int,
        val chatTitle: String,
        val chatType: ChatType
    ): ClientAction

    @Serializable
    class RequestChatMessages(
        val chatId: Int,
    ): ClientAction
}