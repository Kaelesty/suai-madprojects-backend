package data.repos

import data.schemas.GithubService
import domain.GithubTokensRepo
import domain.GithubTokensRepo.Token
import shared_domain.entities.UserMeta

class GithubTokensRepoImpl(
    private val githubService: GithubService
): GithubTokensRepo {

    private val accessLifetimeMillis: Long = 28800 * 1000 // 8 hours
    private val refreshLifetimeMillis: Long = 15897600 * 1000 // 6 months
    private val lifetimeLag: Long = 1000 * 60 * 5

    override suspend fun save(
        refresh: String,
        access: String,
        userId: String,
        githubId: Int,
        avatar: String,
        profileLink: String,
    ) {
        githubService.create(
            userId_ = userId,
            refresh = refresh,
            access = access,
            refreshExpiresMillis_ = System.currentTimeMillis() + refreshLifetimeMillis - lifetimeLag,
            accessExpiresMillis_ = System.currentTimeMillis() + accessLifetimeMillis - lifetimeLag,
            githubId_ = githubId,
            avatar_ = avatar,
            profileLink_ = profileLink
        )
    }

    override suspend fun updateTokens(userId: String, access: String, refresh: String) {
        githubService.updateTokens(
            userId, access, refresh
        )
    }

    override suspend fun getAccessToken(userId: String): Token? {
        val token = githubService.getAccessToken(userId)
        if (token == null) return null
        if (token.second < System.currentTimeMillis()) {
            return Token.Expired
        }
        return Token.Alive(token.first)
    }

    override suspend fun getRefreshToken(userId: String): Token? {
        val token = githubService.getRefreshToken(userId)
        if (token == null) return null
        if (token.second < System.currentTimeMillis()) {
            return Token.Expired
        }
        return Token.Alive(token.first)
    }

    override suspend fun getUserMeta(githubUserId: Int): UserMeta? {
        return githubService.getUserMeta(githubUserId)
    }

    override suspend fun getUserMeta(userId: String): UserMeta? {
        return githubService.getUserMeta(userId)
    }
}