package data.repos

import data.schemas.GithubService
import domain.GithubTokensRepo

class GithubTokensRepoImpl(
    private val githubService: GithubService
): GithubTokensRepo {

    private val accessLifetimeMillis: Long = 28800 * 1000 // 8 hours
    private val refreshLifetimeMillis: Long = 15897600 * 1000 // 6 months
    private val lifetimeLag: Long = 1000 * 60 * 5

    override suspend fun saveTokens(refresh: String, access: String, userId: Int) {
        githubService.create(
            userId_ = userId,
            refresh = refresh,
            access = access,
            refreshExpiresMillis_ = System.currentTimeMillis() + refreshLifetimeMillis - lifetimeLag,
            accessExpiresMillis_ = System.currentTimeMillis() + accessLifetimeMillis - lifetimeLag,
        )
    }
}