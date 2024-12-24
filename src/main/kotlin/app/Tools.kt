package app

import domain.GithubTokensRepo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import shared_domain.entities.GithubTokens

class GithubTokenUtil(
    private val githubTokensRepo: GithubTokensRepo,
    private val httpClient: HttpClient,
    private val config: app.config.Config
) {
    suspend fun getGithubAccessToken(userId: String): String? {

        val accessToken = githubTokensRepo.getAccessToken(userId)
        if (accessToken is GithubTokensRepo.Token.Alive) {
            return accessToken.token
        }

        githubTokensRepo.getRefreshToken(userId)?.let { refresh ->
            if (refresh is GithubTokensRepo.Token.Expired) return null
            val gConfig = config.github
            val response = httpClient.get("https://github.com/login/oauth/access_token") {
                parameter("client_id", gConfig.clientId)
                parameter("client_secret", gConfig.clientSecret)
                parameter("grant_type", "refresh_token")
                parameter("refresh_token", (refresh as GithubTokensRepo.Token.Alive).token)
            }
            if (response.status == HttpStatusCode.OK) {
                val str = response.body<String>()
                str.toString()
                val body = response.body<GithubTokens>()
                //val (accessToken, refreshToken) = extractTokens(body)

                githubTokensRepo.updateTokens(
                    access = body.access_token,
                    refresh = body.refresh_token,
                    userId = userId,
                )
                return body.access_token
            } else return null
        }
        return null
    }
}