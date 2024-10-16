package shared_domain.repos

import entities.Message


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