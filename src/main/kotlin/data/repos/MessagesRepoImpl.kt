package data.repos

import data.schemas.MessageService
import data.schemas.UnreadMessageService
import domain.IntegrationService
import entities.Message
import shared_domain.repos.MessagesRepository

class MessagesRepoImpl(
    private val messageService: MessageService,
    private val unreadMessageService: UnreadMessageService,
    private val integrationService: IntegrationService,
): MessagesRepository {

    override suspend fun getLastMessage(chatId: Int): Message? {
        return messageService.getChatMessages(chatId).lastOrNull()
    }

    override suspend fun createMessage(chatId: Int, senderId: Int, text: String): Message {

        val chatUsersId: List<Int> = integrationService.getChatMembersIds(chatId)

        return messageService.create(
            chatId_ = chatId,
            senderId_ = senderId,
            text_ = text,
            createTimeMillis_ = System.currentTimeMillis()
        ).also { message ->
            chatUsersId.forEach {
                if (it != senderId) {
                    unreadMessageService.create(
                        messageId_ = message.id,
                        userId_ = it,
                        chatId_ = chatId
                    )
                }
            }
        }
    }

    override suspend fun getChatMessages(chatId: Int): List<Message> {
        return messageService.getChatMessages(chatId)
    }

    override suspend fun readMessage(messageId: Int, userId: Int) {
        unreadMessageService.deleteUnreadMessage(
            messageId_ = messageId,
            userId_ = userId
        )
    }

    override suspend fun getUnreadMessagesId(chatId: Int, userId: Int): List<Int> {
        return unreadMessageService.getUserUnreadMessages(
            userId_ = userId,
            chatId_ = chatId
        ).map {
            it.messageId
        }
    }
}