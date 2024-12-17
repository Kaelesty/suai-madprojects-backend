package app.features.project

import domain.RepositoriesRepo
import domain.activity.ActivityRepo
import domain.activity.ActivityType
import domain.profile.ProfileRepo
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

    suspend fun getProject(rc: RoutingContext)

    suspend fun removeRepository(rc: RoutingContext)

    suspend fun addRepository(rc: RoutingContext)

    suspend fun updateProjectMeta(rc: RoutingContext)

    suspend fun removeMember(rc: RoutingContext)

    suspend fun deleteProject(rc: RoutingContext)
}

class ProjectsFeatureImpl(
    private val projectRepo: ProjectRepo,
    private val repositoriesRepo: RepositoriesRepo,
    private val activityRepo: ActivityRepo,
    private val profileRepo: ProfileRepo
) : ProjectsFeature {

    override suspend fun removeMember(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val memberId = call.parameters["memberId"]
            val projectId = call.parameters["projectId"]
            if (
                projectId == null || memberId == null
                || !projectRepo.checkUserIsCreator(userId, projectId)
                || projectRepo.checkUserIsCreator(memberId, projectId)
            ) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            projectRepo.removeProjectMember(memberId, projectId)

            val memberProfile = profileRepo.getSharedById(memberId)

            activityRepo.recordActivity(
                projectId = projectId,
                actorId = userId,
                targetTitle = if (memberProfile != null) "${memberProfile.lastName} ${memberProfile.firstName}" else "",
                targetId = memberId,
                type = ActivityType.MemberRemove
            )

            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun deleteProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (
                projectId == null
                || !projectRepo.checkUserIsCreator(userId, projectId)
            ) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            projectRepo.deleteProject(projectId)
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun updateProjectMeta(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<UpdateProjectMetaRequest>()
            if (!projectRepo.checkUserInProject(userId, request.projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            with(request) {
                projectRepo.updateProjectMeta(
                    projectId, title, desc
                )
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun removeRepository(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            val repoId = call.parameters["repoId"]
            if (
                projectId == null || repoId == null ||
                !projectRepo.checkUserInProject(userId, projectId)
            ) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val repo = repositoriesRepo.getById(repoId)
            repositoriesRepo.removeRepo(repoId)
            activityRepo.recordActivity(
                projectId = projectId,
                actorId = userId,
                targetTitle = repo.link,
                targetId = repo.id,
                type = ActivityType.RepoUnbind
            )
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun addRepository(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            val repoLink = call.parameters["repoLink"]
            if (
                projectId == null || repoLink == null ||
                !projectRepo.checkUserInProject(userId, projectId)
            ) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val projectRepos = repositoriesRepo.getProjectRepos(projectId)
                .map { it.link }

            if (!projectRepos.contains(repoLink)) {
                val newId = repositoriesRepo.addRepo(projectId, repoLink)
                activityRepo.recordActivity(
                    projectId = projectId,
                    actorId = userId,
                    targetTitle = repoLink,
                    targetId = newId,
                    type = ActivityType.RepoBind
                )
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun getProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectRepo.checkUserInProject(userId, projectId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val project = projectRepo.getProject(projectId, userId)
            call.respondText(
                text = Json.encodeToString(project),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

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
                request = request.copy(
                    repoLinks = request.repoLinks.distinct()
                ),
                userId = userId,
            )
            call.respond(HttpStatusCode.OK, mapOf<String, String>("projectId" to projectId))
        }
    }
}