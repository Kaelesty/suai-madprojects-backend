package org.example.domain.entities

import domain.entities.Message
import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: Int,
    val title: String,
    val lastMessage: Message?,
    val unreadMessagesCount: Int,
    val chatType: ChatType
)