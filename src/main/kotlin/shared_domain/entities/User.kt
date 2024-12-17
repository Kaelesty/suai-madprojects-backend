package entities

import kotlinx.serialization.Serializable

data class User(
    val id: String,
    val type: UserType,
)

@Serializable
data class ChatSender(
    val id: String,
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val avatar: String?
)