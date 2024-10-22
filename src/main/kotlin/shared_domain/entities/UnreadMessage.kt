package entities

data class UnreadMessage(
    val id: Int,
    val userId: Int,
    val messageId: Int,
)