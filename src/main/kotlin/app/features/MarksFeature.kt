package app.features

import data.schemas.ProjectCuratorshipService
import domain.CuratorshipRepo
import domain.MarksRepo
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

interface MarksFeature {

    suspend fun markProject(rc: RoutingContext)

    suspend fun getProjectMark(rc: RoutingContext)

}

class MarksFeatureImpl(
    private val marksRepo: MarksRepo,
    private val projectRepo: ProjectRepo,
    private val curatorshipRepo: CuratorshipRepo,
): MarksFeature {

    override suspend fun markProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            val mark = call.parameters["mark"]

            if (projectId == null || mark == null || !projectRepo.checkUserIsProjectCurator(projectId, userId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            marksRepo.markProject(projectId, mark.toInt())
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun getProjectMark(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]

            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val mark = marksRepo.getProjectMark(projectId)

            call.respondText(
                text = Json.encodeToString(
                    mapOf("mark" to mark)
                ),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }
}