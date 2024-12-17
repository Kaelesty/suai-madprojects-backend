package app.features.sprints

import domain.project.ProjectRepo
import domain.sprints.SprintsRepo
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

interface SprintsFeature {

    suspend fun createSprint(rc: RoutingContext)
}

class SprintsFeatureImpl(
    private val projectRepo: ProjectRepo,
    private val sprintsRepo: SprintsRepo,
): SprintsFeature {

    override suspend fun createSprint(rc: RoutingContext) {
       with(rc) {
           val principal = call.principal<JWTPrincipal>()
           val userId = principal!!.payload.getClaim("userId").asString()
           val request = call.receive<CreateSprintRequest>()
           if (!projectRepo.checkUserInProject(userId, request.projectId)) {
               call.respond(HttpStatusCode.NotFound)
           }
           val sprintId = sprintsRepo.createSprint(request)
           call.respondText(
               text = Json.encodeToString(
                   mapOf("sprintId" to sprintId)
               ),
               contentType = ContentType.Application.Json,
               status = HttpStatusCode.OK
           )
       }
    }
}