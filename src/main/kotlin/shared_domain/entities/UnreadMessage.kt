package entities

data class UnreadMessage(
    val id: Int,
    val userId: String,
    val messageId: Int,
)