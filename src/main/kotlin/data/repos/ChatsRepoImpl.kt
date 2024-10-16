package data.repos

import data.schemas.ChatService
import entities.Chat
import entities.ChatType
import entities.UserType
import shared_domain.repos.ChatsRepository

class ChatsRepoImpl(
    private val chatService: ChatService
): ChatsRepository {

    override suspend fun createChat(title: String, projectId: Int, chatType: ChatType): Chat {
        val new = chatService.create(
            projectId_ = projectId,
            chatType_ = chatType,
            title_ = title
        )
        return new
    }

    override suspend fun getProjectChats(projectId: Int, userType: UserType): List<Chat> {
        return when (userType) {
            UserType.DEFAULT -> {
                chatService.getProjectChats(projectId, ChatType.Public) +
                        chatService.getProjectChats(projectId, ChatType.MembersPrivate)
            }
            UserType.CURATOR -> {
                chatService.getProjectChats(projectId, ChatType.Public) +
                        chatService.getProjectChats(projectId, ChatType.CuratorPrivate)
            }
        }
    }
}