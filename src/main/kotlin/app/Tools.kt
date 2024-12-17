package app

import domain.project.ProjectRepo
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingContext

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