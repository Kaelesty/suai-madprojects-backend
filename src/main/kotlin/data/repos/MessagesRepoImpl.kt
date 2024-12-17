package data.repos

import data.schemas.ChatService
import data.schemas.MessageService
import data.schemas.ProjectCuratorshipService
import data.schemas.ProjectMembershipService
import data.schemas.UnreadMessageService
import domain.IntegrationService
import entities.ChatType
import entities.Message
import entities.UserType
import shared_domain.repos.MessagesRepository

class MessagesRepoImpl(
    private val messageService: MessageService,
    private val unreadMessageService: UnreadMessageService,
    private val integrationService: IntegrationService,
    private val projectMembershipService: ProjectMembershipService,
    private val chatService: ChatService,
    private val projectCuratorshipService: ProjectCuratorshipService
): MessagesRepository {

    override suspend fun getLastMessage(chatId: Int): Message? {
        return messageService.getChatMessages(chatId).lastOrNull()
    }

    override suspend fun createMessage(chatId: Int, senderId: String, text: String, projectId: Int): Message {

        val chatUsersId = projectMembershipService.getProjectUserIds(projectId.toString()).toMutableList()
        val chat = chatService.getById(chatId)

        if (chat.chatType != ChatType.MembersPrivate) {
            projectCuratorshipService.getProjectCurator(projectId).forEach {
                chatUsersId.add(it)
            }
        }

        return messageService.create(
            chatId_ = chatId,
            senderId_ = senderId,
            text_ = text,
            createTimeMillis_ = System.currentTimeMillis()
        ).also { message ->
            if (chat.chatType == ChatType.CuratorPrivate) return@also
            chatUsersId.forEach {
                if (it != senderId.toInt()) {
                    unreadMessageService.create(
                        messageId_ = message.id,
                        userId_ = it.toString(),
                        chatId_ = chatId
                    )
                }
            }
        }
    }

    override suspend fun getChatMessages(chatId: Int, userType: UserType): List<Message> {
        val chat = chatService.getById(chatId)
        if (
            (userType == UserType.CURATOR && chat.chatType == ChatType.MembersPrivate) ||
            (userType == UserType.COMMON && chat.chatType == ChatType.CuratorPrivate)
        ) return listOf()
        return messageService.getChatMessages(chatId)
    }

    override suspend fun readMessage(messageId: Int, userId: String) {
        unreadMessageService.deleteUnreadMessage(
            messageId_ = messageId,
            userId_ = userId
        )
    }

    override suspend fun getUnreadMessagesId(chatId: Int, userId: String): List<Int> {
        return unreadMessageService.getUserUnreadMessages(
            userId_ = userId,
            chatId_ = chatId
        ).map {
            it.messageId
        }
    }
}