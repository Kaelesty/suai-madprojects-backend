package org.example.domain.entities

import domain.entities.Message
import kotlinx.serialization.Serializable

@Serializable
sealed interface ServerAction {

    @Serializable
    class NewMessage(
        val chatId: Int,
        val message: Message,
    ): ServerAction

    @Serializable
    class SendChatsList(
        val chatId: Int,
        val chatTitle: String,
        val lastMessage: Message,
        val unreadMessagesCount: Int
    ): ServerAction
    // On connect to WS

    @Serializable
    class NewChat(
        val chat: Chat,
    ): ServerAction

    @Serializable
    class SendChatMessages(
        val chatId: Int,
        val messages: List<Message>
    ): ServerAction
}