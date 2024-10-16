package domain.repos

import domain.entities.Message

interface MessagesRepository {

    suspend fun createMessage(
        chatId: Int,
        senderId: Int,
        text: String,
    ): Message

    suspend fun getChatMessages(
        chatId: Int,
    ): List<Message>
}