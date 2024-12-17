package data.repos

import data.schemas.GithubService
import domain.GithubTokensRepo
import domain.GithubTokensRepo.Token
import shared_domain.entities.GithubUserMeta

class GithubTokensRepoImpl(
    private val githubService: GithubService
): GithubTokensRepo {

    private val accessLifetimeMillis: Long = 28800L * 1000L // 8 hours
    private val refreshLifetimeMillis: Long = 15897600L * 1000L // 6 months
    private val lifetimeLag: Long = 1000L * 60L * 5L

    override suspend fun save(
        refresh: String,
        access: String,
        userId: String,
        githubId: Int,
        avatar: String,
        profileLink: String,
    ) {

        val refreshExpires = System.currentTimeMillis() + refreshLifetimeMillis - lifetimeLag
        val accessExpires = System.currentTimeMillis() + accessLifetimeMillis - lifetimeLag

        githubService.create(
            userId_ = userId,
            refresh = refresh,
            access = access,
            refreshExpiresMillis_ = refreshExpires,
            accessExpiresMillis_ = accessExpires,
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

    override suspend fun getUserMeta(githubUserId: Int): GithubUserMeta? {
        return githubService.getUserMeta(githubUserId)
    }

    override suspend fun getUserMeta(userId: String): GithubUserMeta? {
        return githubService.getUserMeta(userId)
    }
}