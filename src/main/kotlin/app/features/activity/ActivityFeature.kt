package app.features.activity

import domain.activity.ActivityRepo
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

interface ActivityFeature {

    suspend fun getActivity(rc: RoutingContext)
}

class ActivityFeatureImpl(
    private val profileRepo: ProfileRepo,
    private val activityRepo: ActivityRepo,
    private val projectRepo: ProjectRepo
): ActivityFeature {

    override suspend fun getActivity(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            val count = call.parameters["count"]?.toInt()
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val activities = activityRepo.getProjectActivity(projectId, count)
            val actors = activities.map { it.actorId }
                .distinct()
                .filterNotNull()
                .map {
                    val profile = profileRepo.getSharedById(it)
                    if (profile == null) {
                        null
                    }
                    else {
                        it to profile
                    }
                }
                .filterNotNull()

            call.respondText(
                text = Json.encodeToString(
                    ActivityResponse(
                        activities = activities,
                        actors = actors.map { it.first to it.second }.toMap()
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}