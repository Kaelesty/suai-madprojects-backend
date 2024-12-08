package domain

interface GithubTokensRepo {

    suspend fun saveTokens(refresh: String, access: String, userId: Int)

    suspend fun getAccessToken(userId: Int): String?
}