package entities

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: Int,
    val text: String,
    val senderId: Int,
    val time: Long,
)