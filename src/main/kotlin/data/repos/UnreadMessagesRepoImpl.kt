package data.repos

import data.schemas.UnreadMessageService
import domain.UnreadMessagesRepository

class UnreadMessagesRepoImpl(
    private val unreadMessageService: UnreadMessageService
): UnreadMessagesRepository {

    override suspend fun getUnreadMessagesCount(userId: Int, chatId: Int): Int {
        return unreadMessageService.getUserUnreadMessages(
            userId_ = userId,
            chatId_ = chatId
        ).size
    }
}