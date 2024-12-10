package domain

interface UnreadMessagesRepository {

    suspend fun getUnreadMessagesCount(userId: String, chatId: Int): Int

    suspend fun readMessagesBefore(messageId: Int, chatId: Int, userId: String)
}