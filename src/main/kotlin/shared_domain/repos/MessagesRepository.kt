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

    suspend fun readMessage(
        messageId: Int,
        userId: Int,
    )

    suspend fun getUnreadMessagesId(
        chatId: Int,
        userId: Int,
    ): List<Int>

    suspend fun getLastMessage(
        chatId: Int
    ): Message?
}