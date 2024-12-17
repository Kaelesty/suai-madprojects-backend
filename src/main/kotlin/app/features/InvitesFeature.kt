package app.features

import data.schemas.ProjectMembershipService
import domain.InvitesRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface InvitesFeature {

    suspend fun getProjectInvite(rc: RoutingContext)

    suspend fun refreshProjectInvite(rc: RoutingContext)

    suspend fun useInvite(rc: RoutingContext)
}

class InvitesFeatureImpl(
    private val invitesRepo: InvitesRepo,
    private val projectMembershipService: ProjectMembershipService,
): InvitesFeature {

    override suspend fun getProjectInvite(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectMembershipService.isUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val invite = invitesRepo.getProjectInvite(projectId)
            call.respondText(
                text = Json.encodeToString(
                    mapOf(
                        "invite" to invite
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun refreshProjectInvite(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectMembershipService.isUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val invite = invitesRepo.refreshProjectInvite(projectId)
            call.respondText(
                text = Json.encodeToString(
                    mapOf(
                        "invite" to invite
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun useInvite(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val invite = call.parameters["invite"]

            if (invite == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val projectId = invitesRepo.useInvite(invite, userId)

            if (projectId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            call.respondText(
                text = Json.encodeToString(
                    mapOf(
                        "projectId" to projectId
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}