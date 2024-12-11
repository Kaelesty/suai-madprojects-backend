package domain

import entities.User

interface IntegrationService {

    fun getUserFromJWT(jwt: String): User?

    fun getProjectUsers(projectId: Int): List<User>

    fun getChatMembersIds(chatId: Int): List<String>
}