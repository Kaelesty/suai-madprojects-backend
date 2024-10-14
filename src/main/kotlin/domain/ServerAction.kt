import kotlinx.serialization.Serializable

@Serializable
sealed class ServerAction {

    @Serializable
    class NewMessage(val chatId: Int, val message: String, val userId: Int): ServerAction()

    @Serializable
    class TypingStarted(val chatId: Int, val userId: Int): ServerAction()
}