package app.features

import domain.project.ProjectRepo
import domain.sprints.CreateSprintRequest
import domain.sprints.SprintsRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

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
           sprintsRepo.createSprint(request)
       }
    }
}