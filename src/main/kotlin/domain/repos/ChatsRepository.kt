package domain.repos

import domain.entities.UserType
import org.example.domain.entities.Chat
import org.example.domain.entities.ChatType

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