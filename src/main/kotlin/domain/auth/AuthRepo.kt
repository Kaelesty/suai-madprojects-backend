package domain.auth

interface AuthRepo {

    suspend fun login(email: String, password: String): LoginResult

    suspend fun createCommonProfile(
        username: String,
        lastName: String,
        firstName: String,
        secondName: String,
        group: String,
        email: String,
        password: String
    ): String

    suspend fun createCuratorProfile(
        username: String,
        lastName: String,
        firstName: String,
        secondName: String,
        grade: String,
        email: String,
        password: String
    ): String

    suspend fun checkUnique(
        email: String, username: String,
    ): CheckUniqueResult

    suspend fun generateRefreshRequest(userId: String, expiresAt: Long): String

    suspend fun checkRefreshRequest(request: String): UserContext?
}