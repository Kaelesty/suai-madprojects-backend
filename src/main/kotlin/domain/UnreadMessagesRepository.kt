package domain

interface UnreadMessagesRepository {

    suspend fun getUnreadMessagesCount(userId: Int, chatId: Int): Int
}