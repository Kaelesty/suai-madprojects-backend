package entities

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: Int,
    val title: String,
    val lastMessage: Message?,
    val unreadMessagesCount: Int,
    val chatType: ChatType
)