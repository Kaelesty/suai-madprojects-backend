package app.features.sprints

import domain.KanbanRepository
import domain.activity.ActivityRepo
import domain.activity.ActivityType
import domain.project.ProjectRepo
import domain.sprints.SprintMeta
import domain.sprints.SprintView
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

    suspend fun getProjectSprints(rc: RoutingContext)

    suspend fun finishSprint(rc: RoutingContext)

    suspend fun getSprint(rc: RoutingContext)

    suspend fun updateSprint(rc: RoutingContext)
}

class SprintsFeatureImpl(
    private val projectRepo: ProjectRepo,
    private val sprintsRepo: SprintsRepo,
    private val activityRepo: ActivityRepo,
    private val kanbanRepo: KanbanRepository
) : SprintsFeature {

    override suspend fun updateSprint(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val request = call.receive<UpdateSprintRequest>()

            val projectId = sprintsRepo.getSprintProjectId(request.sprintId)
            if (!projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            sprintsRepo.updateSprint(request)
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun getSprint(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val sprintId = call.parameters["sprintId"]
            if (sprintId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val projectId = sprintsRepo.getSprintProjectId(sprintId)
            if (!projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val sprint = sprintsRepo.getSprint(sprintId)
            val kanban = kanbanRepo.getKanban(projectId.toInt(), onlyKardIds = sprint.kardIds)
            call.respondText(
                text = Json.encodeToString(
                    SprintView(
                        meta = sprint.meta,
                        kanban = kanban
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun finishSprint(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val sprintId = call.parameters["sprintId"]
            if (sprintId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val projectId = sprintsRepo.getSprintProjectId(sprintId)
            if (!projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val sprint = sprintsRepo.getSprint(sprintId)

            sprintsRepo.finishSprint(sprintId)
            activityRepo.recordActivity(
                projectId = projectId,
                actorId = userId,
                targetTitle = sprint.meta.title,
                targetId = sprintId,
                type = ActivityType.SprintFinish
            )
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun getProjectSprints(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val response = sprintsRepo.getProjectSprints(projectId)
            call.respondText(
                text = Json.encodeToString(response),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun createSprint(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<CreateSprintRequest>()
            if (!projectRepo.checkUserInProject(userId, request.projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val sprintId = sprintsRepo.createSprint(request)

            activityRepo.recordActivity(
                projectId = request.projectId,
                actorId = userId,
                targetTitle = request.title,
                targetId = sprintId,
                type = ActivityType.SprintStart
            )

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