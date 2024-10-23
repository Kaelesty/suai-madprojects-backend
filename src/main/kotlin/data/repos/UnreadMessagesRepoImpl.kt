package data.repos

import data.schemas.UnreadMessageService
import domain.UnreadMessagesRepository

class UnreadMessagesRepoImpl(
    private val unreadMessageService: UnreadMessageService
) : UnreadMessagesRepository {

    override suspend fun getUnreadMessagesCount(userId: Int, chatId: Int): Int {
        return unreadMessageService.getUserUnreadMessages(
            userId_ = userId,
            chatId_ = chatId
        ).size
    }

    override suspend fun readMessagesBefore(messageId: Int, chatId: Int, userId: Int) {
        val unreads = unreadMessageService.getUserUnreadMessages(userId_ = userId, chatId_ = chatId)
        unreads
            .filter { it.messageId <= messageId }
            .forEach {
                unreadMessageService.deleteUnreadMessage(userId_ = userId, messageId_ = it.messageId)
            }
    }
}