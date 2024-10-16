package shared_domain.repos

import entities.Chat
import entities.ChatType
import entities.UserType

interface ChatsRepository {

    suspend fun createChat(
        title: String,
        projectId: Int,
        chatType: ChatType,
    ): Chat

    suspend fun getProjectChats(
        projectId: Int,
        userType: UserType,
    ): List<Chat>
}