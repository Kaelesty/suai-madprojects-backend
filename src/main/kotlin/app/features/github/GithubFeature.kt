package app.features.github

import app.Config
import com.auth0.jwt.JWTVerifier
import domain.GithubTokensRepo
import domain.RepositoriesRepo
import domain.profile.ProfileRepo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import shared_domain.entities.Branch
import shared_domain.entities.BranchCommit
import shared_domain.entities.BranchCommitView
import shared_domain.entities.BranchCommits
import shared_domain.entities.GithubTokens
import shared_domain.entities.GithubUser
import shared_domain.entities.RepoBranchView
import shared_domain.entities.RepoView
import kotlin.collections.forEach

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
): GithubFeature {

    private val githubAuthLink =
        "https://github.com/login/oauth/access_token?client_id=${Config.Github.clientId}&client_secret=${Config.Github.clientSecret}"
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
            val githubJwt = getGithubAccessToken(userId)

            if (githubJwt == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }

            val projectRepos = repositoriesRepo.getProjectRepos(projectId)
            val repos = mutableListOf<RepoView>()

            projectRepos.forEach {
                val repoBranches = mutableListOf<RepoBranchView>()
                val parts = it.link.split("/").reversed()
                val response = httpClient.get("$githubRepoLink/${parts[1]}/${parts[0]}/branches") {
                    header("Authorization", "Bearer $githubJwt")
                }
                if (response.status == HttpStatusCode.OK) {
                    try {
                        val body = response.body<List<Branch>>()
                        body.forEach { branch ->
                            repoBranches.add(
                                RepoBranchView(
                                    name = "${parts[0]}/${branch.name}",
                                    sha = branch.data.sha,
                                )
                            )
                        }

                        repos.add(
                            RepoView(
                                name = "${parts[1]}/${parts[0]}",
                                repoBranches
                            )
                        )
                    } catch (e: Exception) {
                        e
                        call.respond(HttpStatusCode.Conflict)
                        return
                    }
                } else {
                    call.respond(HttpStatusCode.ServiceUnavailable)
                    return
                }
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

            val githubJwt = getGithubAccessToken(userId)

            if (githubJwt == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }

            val response = httpClient.get(
                "$githubRepoLink/$repo/commits?per_page=1000&sha=$sha"
            ) {
                header("Authorization", "Bearer $githubJwt")
            }
            if (response.status == HttpStatusCode.OK) {
                try {
                    val body = response.body<List<BranchCommit>>()
                    val commits = body.map {
                        BranchCommitView(
                            sha = it.sha,
                            authorGithubId = it.author.id,
                            date = it.data.author.date,
                            message = it.data.message
                        )
                    }

                    val authorIds = commits.map { it.authorGithubId }
                    val authors = authorIds.distinct().map { githubUserId ->
                        Commiter(
                            githubMeta = githubTokensRepo.getUserMeta(githubUserId),
                            profile = profileRepo.getSharedByGithubId(githubUserId)
                        )
                    }

                    call.respondText(
                        Json.encodeToString(
                            BranchCommits(
                                commits = commits,
                                authors = authors
                            )
                        ), ContentType.Application.Json, HttpStatusCode.OK
                    )

                } catch (_: Exception) {
                    call.respond(HttpStatusCode.Conflict)
                }
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable)
            }
        }
    }

    private suspend fun getGithubAccessToken(userId: String): String? {

        val accessToken = githubTokensRepo.getAccessToken(userId)
        if (accessToken is GithubTokensRepo.Token.Alive) {
            return accessToken.token
        }

        githubTokensRepo.getRefreshToken(userId)?.let { refresh ->
            if (refresh is GithubTokensRepo.Token.Expired) return null
            val response = httpClient.get("https://github.com/login/oauth/") {
                parameter("client_id", Config.Github.clientId)
                parameter("client_secret", Config.Github.clientSecret)
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