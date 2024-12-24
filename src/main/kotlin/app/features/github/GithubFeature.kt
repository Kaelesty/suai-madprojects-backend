package app.features.github

import app.GithubTokenUtil
import com.auth0.jwt.JWTVerifier
import domain.BranchesRepo
import domain.GithubTokensRepo
import domain.RepositoriesRepo
import domain.profile.ProfileRepo
import domain.profile.SharedProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import shared_domain.entities.GithubTokens
import shared_domain.entities.GithubUser

interface GithubFeature {

    suspend fun getUserMeta(rc: RoutingContext)

    suspend fun getRepoBranchContent(rc: RoutingContext)

    suspend fun getProjectRepoBranches(rc: RoutingContext)

    suspend fun verifyRepoLink(rc: RoutingContext)

    suspend fun proceedGithubApiCallback(rc: RoutingContext)
}

class GithubFeatureImpl(
    private val githubTokensRepo: GithubTokensRepo,
    private val repositoriesRepo: RepositoriesRepo,
    private val httpClient: HttpClient,
    private val jwt: JWTVerifier,
    private val profileRepo: ProfileRepo,
    private val branchesRepo: BranchesRepo,
    private val tokenUtil: GithubTokenUtil,
    private val config: app.config.Config
): GithubFeature {

    private val githubAuthLink =
        "https://github.com/login/oauth/access_token?client_id=${config.github.clientId}&client_secret=${config.github.clientSecret}"
    private val githubRepoLink = "https://api.github.com/repos"

    override suspend fun proceedGithubApiCallback(rc: RoutingContext) {
        with(rc) {

            val githubCode = call.parameters["code"] ?: call.respond(
                HttpStatusCode.Unauthorized,
                "Failed to parse github code"
            )
            val userId = call.parameters["state"]
                ?.let {
                    jwt.verify(it).getClaim("userId").asString()
                }
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to get tokens from code")
                return
            }



            val response = httpClient.get("$githubAuthLink&code=$githubCode")
            if (response.status == HttpStatusCode.OK) {
                val body = try {
                    response.body<GithubTokens>()
                } catch (e: Exception) {
                    e
                    call.respond(HttpStatusCode.SeeOther, "Bad code")
                    return
                }

                val metaResponse = httpClient.get("https://api.github.com/user") {
                    header("Authorization", "Bearer ${body.access_token}")
                }
                if (metaResponse.status != HttpStatusCode.OK) {
                    call.respond(
                        HttpStatusCode.FailedDependency,
                        "Failed to load user meta via secondary request"
                    )
                }

                try {
                    val githubUser = metaResponse.body<GithubUser>()
                    githubTokensRepo.save(
                        access = body.access_token,
                        refresh = body.refresh_token,
                        userId = userId,
                        githubId = githubUser.id,
                        avatar = githubUser.avatarUrl,
                        profileLink = githubUser.profileLink
                    )
                } catch (e: Exception) {
                    e
                    call.respond(
                        HttpStatusCode.FailedDependency,
                        "Failed to parse user meta from secondary request"
                    )
                    return
                }


                call.respond(HttpStatusCode.OK, "Tokens are recorded")
            } else call.respond(HttpStatusCode.Unauthorized, "Failed to get tokens from code")
        }
    }

    override suspend fun verifyRepoLink(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val link = call.parameters["repolink"]
            if (link == null) {
                call.respond(HttpStatusCode.BadRequest, "Bad parameters")
                return
            }
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to parse jwt")
                return
            }
            val githubJwt = githubTokensRepo.getAccessToken(userId)

            if (githubJwt == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }

            val parts = link.split("/").reversed()
            val response = httpClient.get("$githubRepoLink/${parts[1]}/${parts[0]}") {
                header("Authentication", "Bearer $githubJwt")
            }
            if (response.status != HttpStatusCode.OK) {
                call.respond(HttpStatusCode.NotFound, "Invalid repolink")
                return
            }
            val body = response.body<VerifyResponse>()
            if (body.isPrivate) {
                call.respond(HttpStatusCode.MethodNotAllowed, "Private repo")
                return
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun getProjectRepoBranches(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null) {
                call.respond(HttpStatusCode.BadRequest, "Bad parameters")
                return
            }
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to parse jwt")
                return
            }
            val githubJwt = tokenUtil.getGithubAccessToken(userId)

            if (githubJwt == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }

            val repos = branchesRepo.getProjectRepoBranches(projectId, githubJwt)

            if (repos == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            call.respondText(
                Json.encodeToString(
                    repos
                ), ContentType.Application.Json, HttpStatusCode.OK
            )
        }
    }

    override suspend fun getUserMeta(rc: RoutingContext) {
        with(rc) {
            val paramUserId = call.parameters["userId"]
            val githubUserId = call.parameters["githubUserId"]
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val meta = githubTokensRepo.getUserMeta(
                if (paramUserId != null) userId else if (githubUserId != null) githubUserId
                else userId
            )

            if (meta == null) {
                call.respond(HttpStatusCode.NotFound, "User meta was not found")
            }

            call.respondText(Json.encodeToString(meta), ContentType.Application.Json, HttpStatusCode.OK)

        }
    }

    override suspend fun getRepoBranchContent(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val sha = call.parameters["sha"]
            val repo = call.parameters["repoName"]
            if (sha == null || repo == null) {
                call.respond(HttpStatusCode.BadRequest, "Bad parameters")
                return
            }
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Failed to parse jwt")
                return
            }

            val githubJwt = tokenUtil.getGithubAccessToken(userId)

            if (githubJwt == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }

            val branchCommits = branchesRepo.getRepoBranchContent(
                sha = sha,
                repoName = repo,
                githubJwt = githubJwt,
                profileMaker = { profileRepo.getSharedByGithubId(it)?.let {
                    SharedProfile(
                        firstName = it.firstName,
                        secondName = it.secondName,
                        lastName = it.lastName,
                    )
                } }

            )

            if (branchCommits == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            call.respondText(
                Json.encodeToString(
                    branchCommits
                ), ContentType.Application.Json, HttpStatusCode.OK
            )
        }
    }
}