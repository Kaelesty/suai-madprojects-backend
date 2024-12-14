package app.features

import domain.GithubTokensRepo
import domain.profile.CommonProfileResponse
import domain.profile.ProfileRepo
import domain.project.ProjectRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ProfileFeature {

    suspend fun getCommonProfile(rc: RoutingContext)
}

class ProfileFeatureImpl(
    private val profileRepo: ProfileRepo,
    private val githubTokensRepo: GithubTokensRepo,
    private val projectsRepo: ProjectRepo
): ProfileFeature {

    override suspend fun getCommonProfile(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val user = profileRepo.getCommonById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val githubMeta = githubTokensRepo.getUserMeta(userId)

            val projects = projectsRepo.getUserProjects(userId)

            call.respondText(
                text = Json.encodeToString(
                    CommonProfileResponse(
                        firstName = user.data.firstName,
                        lastName = user.data.lastName,
                        secondName = user.data.secondName,
                        email = user.data.email,
                        projects = projects,
                        githubMeta = githubMeta,
                        group = user.group
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}