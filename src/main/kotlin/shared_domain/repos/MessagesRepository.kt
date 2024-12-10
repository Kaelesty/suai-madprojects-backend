package shared_domain.repos

import entities.Message


interface MessagesRepository {

    suspend fun createMessage(
        chatId: Int,
        senderId: String,
        text: String,
    ): Message

    suspend fun getChatMessages(
        chatId: Int,
    ): List<Message>

    suspend fun readMessage(
        messageId: Int,
        userId: String,
    )

    suspend fun getUnreadMessagesId(
        chatId: Int,
        userId: String,
    ): List<Int>

    suspend fun getLastMessage(
        chatId: Int
    ): Message?
}