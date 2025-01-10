package app.features.projectgroups

import domain.profile.ProfileRepo
import domain.project.ProjectStatus
import domain.projectgroups.ProjectsGroupRepo
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ProjectGroupsFeature {

    suspend fun createProjectsGroup(rc: RoutingContext)

    suspend fun getCuratorProjectGroups(rc: RoutingContext)

    suspend fun getGroupProjects(rc: RoutingContext)

    suspend fun getCuratorProjects(rc: RoutingContext)
}

class ProjectGroupsFeatureImpl(
    private val projectsGroupRepo: ProjectsGroupRepo,
    private val profileRepo: ProfileRepo,
): ProjectGroupsFeature {

    override suspend fun getCuratorProjects(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }

            val projectGroupId = call.parameters["projectGroupId"]?.let {
                if (it == "null") null else it
            }
            val status = call.parameters["status"]?.let {
                if (it == "null") null else ProjectStatus.valueOf(it)
            }
            val marked = call.parameters["marked"]?.let { it == "true" }
            val mark = call.parameters["mark"]?.let {
                if (it == "null") null else it
            }

            val projects = if(projectGroupId != null) {
                projectsGroupRepo.getGroupProjects(projectGroupId)
            } else {
                projectsGroupRepo.getCuratorProjectGroups(userId)
                    .flatMap { projectsGroupRepo.getGroupProjects(it.id) }
            }
                .filter { (marked == null || (marked == true && it.mark != null) || (marked == false && it.mark == null)) }
                .filter { mark == null || (it.mark == mark.toInt()) }
                .filter { (status == null) || (status == it.status) }

            call.respondText(
                text = Json.encodeToString(projects),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    override suspend fun createProjectsGroup(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val request = call.receive<CreateProjectGroupRequest>()
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }
            val new = projectsGroupRepo.createProjectsGroup(
                title = request.title,
                curatorId = userId
            )
            call.respondText(
                text = Json.encodeToString(new),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    override suspend fun getCuratorProjectGroups(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val param = call.parameters["curatorId"]

            val curatorId = if (param == null || param == "null") userId else param

            if (curatorId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val groups = projectsGroupRepo.getCuratorProjectGroups(curatorId)
            call.respondText(
                text = Json.encodeToString(groups),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }

    override suspend fun getGroupProjects(rc: RoutingContext) {
        with (rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val groupId = call.parameters["groupId"]
            if (groupId == null || !projectsGroupRepo.checkIsCuratorGroupOwner(userId, groupId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            if (!profileRepo.checkIsCurator(userId)) {
                call.respond(HttpStatusCode.Locked)
                return
            }
            val groupTitle = projectsGroupRepo.getGroupTitle(groupId)
            val projects = projectsGroupRepo.getGroupProjects(groupId)
            val response = GroupProjectsResponse(
                title = groupTitle,
                projects = projects
            )
            call.respondText(
                text = Json.encodeToString(response),
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }
}