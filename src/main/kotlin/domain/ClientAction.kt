package domain

import kotlinx.serialization.Serializable

@Serializable
sealed class ClientAction(
) {
    @Serializable
    class StartTyping(val jwt: String, val chatId: Int): ClientAction()
    @Serializable
    class SendMessage(val jwt: String, val chatId: Int, val message: String): ClientAction()
}