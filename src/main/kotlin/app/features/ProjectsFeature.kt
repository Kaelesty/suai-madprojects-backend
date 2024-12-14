package app.features

import domain.project.CreateProjectRequest
import domain.project.ProjectRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ProjectsFeature {

    suspend fun getCurators(rc: RoutingContext)

    suspend fun createProject(rc: RoutingContext)
}

class ProjectsFeatureImpl(
    private val projectRepo: ProjectRepo,
): ProjectsFeature {

    override suspend fun getCurators(rc: RoutingContext) {
        with(rc) {
            val curators = projectRepo.getCuratorsList()
            call.respondText(
                text = Json.encodeToString(curators),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun createProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<CreateProjectRequest>()
            val projectId = projectRepo.createProject(
                request = request,
                userId = userId
            )
            call.respond(HttpStatusCode.OK, mapOf<String, String>("projectId" to projectId))
        }
    }
}