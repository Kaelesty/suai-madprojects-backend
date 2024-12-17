package app.features

import domain.InvitesRepo
import domain.activity.ActivityRepo
import domain.activity.ActivityType
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

interface InvitesFeature {

    suspend fun getProjectInvite(rc: RoutingContext)

    suspend fun refreshProjectInvite(rc: RoutingContext)

    suspend fun useInvite(rc: RoutingContext)
}

class InvitesFeatureImpl(
    private val invitesRepo: InvitesRepo,
    private val projectRepo: ProjectRepo,
    private val profileRepo: ProfileRepo,
    private val activityRepo: ActivityRepo,
): InvitesFeature {

    override suspend fun getProjectInvite(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
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
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
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

            val memberProfile = profileRepo.getSharedById(userId)

            activityRepo.recordActivity(
                projectId = projectId,
                actorId = null,
                targetTitle = if (memberProfile != null) "${memberProfile.lastName} ${memberProfile.firstName}" else "",
                targetId = userId,
                type = ActivityType.MemberAdd
            )

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