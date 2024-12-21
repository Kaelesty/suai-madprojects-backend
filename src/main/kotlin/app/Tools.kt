package app

import domain.GithubTokensRepo
import domain.project.ProjectRepo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingContext
import shared_domain.entities.GithubTokens

const val REQUIRE_USER_IN_PROJECT_QUALIFIER = "requireUserInProject"

suspend fun RoutingContext.requireUserInProject(
    projectRepo: ProjectRepo,
    projectId: String,
    returnBlock: () -> Unit
) {

    val principal = call.principal<JWTPrincipal>()
    val userId = principal!!.payload.getClaim("userId").asString()

    if (!projectRepo.checkUserInProject(userId, projectId)) {
        returnBlock()
    }
}

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
            val response = httpClient.get("https://github.com/login/oauth/") {
                parameter("client_id", config.github.clientId)
                parameter("client_secret", config.github.clientSecret)
                parameter("grant_type", "refresh_token")
                parameter("refresh_token", (refresh as GithubTokensRepo.Token.Alive).token)
            }
            if (response.status == HttpStatusCode.OK) {
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