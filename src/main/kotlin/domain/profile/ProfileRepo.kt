package domain.profile

import domain.auth.User

interface ProfileRepo {

    suspend fun getCommonById(userId: String): CommonUser?
}