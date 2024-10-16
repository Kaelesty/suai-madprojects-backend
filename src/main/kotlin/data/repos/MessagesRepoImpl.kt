package data.repos

import data.schemas.MessageService
import entities.Message
import shared_domain.repos.MessagesRepository

class MessagesRepoImpl(
    private val messageService: MessageService
): MessagesRepository {

    override suspend fun createMessage(chatId: Int, senderId: Int, text: String): Message {
        val new = messageService.create(
            chatId_ = chatId,
            senderId_ = senderId,
            text_ = text,
            createTimeMillis_ = System.currentTimeMillis()
        )
        return new
    }

    override suspend fun getChatMessages(chatId: Int): List<Message> {
        return messageService.getChatMessages(chatId)
    }
}