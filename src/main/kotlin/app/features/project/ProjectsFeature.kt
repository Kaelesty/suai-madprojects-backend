package app.features.project

import data.schemas.ProjectMembershipService
import domain.RepositoriesRepo
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
}

class ProjectsFeatureImpl(
    private val projectRepo: ProjectRepo,
    private val projectMembershipService: ProjectMembershipService,
    private val repositoriesRepo: RepositoriesRepo,
) : ProjectsFeature {

    override suspend fun updateProjectMeta(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<UpdateProjectMetaRequest>()
            if (!projectMembershipService.isUserInProject(userId, request.projectId)) {
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
                !projectMembershipService.isUserInProject(userId, projectId)
            ) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            repositoriesRepo.removeRepo(repoId)
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
                !projectMembershipService.isUserInProject(userId, projectId)
            ) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            repositoriesRepo.addRepo(projectId, repoLink)
            call.respond(HttpStatusCode.OK)
        }
    }

    override suspend fun getProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]
            if (projectId == null || !projectMembershipService.isUserInProject(userId, projectId)) {
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
                request = request,
                userId = userId,
            )
            call.respond(HttpStatusCode.OK, mapOf<String, String>("projectId" to projectId))
        }
    }
}