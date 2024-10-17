package domain

import entities.User

interface IntegrationRepository {

    fun getUserFromJWT(jwt: String): User

    fun getProjectUsers(projectId: Int): List<User>
}