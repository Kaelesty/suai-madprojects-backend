package data.repos

import data.schemas.ChatService
import domain.entities.UserType
import domain.repos.ChatsRepository
import org.example.domain.entities.Chat
import org.example.domain.entities.ChatType

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