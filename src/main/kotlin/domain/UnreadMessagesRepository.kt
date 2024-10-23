package domain

interface UnreadMessagesRepository {

    suspend fun getUnreadMessagesCount(userId: Int, chatId: Int): Int

    suspend fun readMessagesBefore(messageId: Int, chatId: Int, userId: Int)
}