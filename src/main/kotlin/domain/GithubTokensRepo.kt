package domain

interface GithubTokensRepo {

    suspend fun saveTokens(refresh: String, access: String, userId: Int)
}