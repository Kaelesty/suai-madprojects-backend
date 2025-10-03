package app.features.analytics

import app.GithubTokenUtil
import domain.BranchesRepo
import domain.project.ProjectRepo
import domain.projectgroups.ProjectInGroupMember
import domain.projectgroups.ProjectsGroupRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface AnalyticsFeature {

    suspend fun getCommitsByProjectInProjectGroup(rc: RoutingContext)

    suspend fun getCommitsByUsersInProject(rc: RoutingContext)

    suspend fun getProjectMarksInProjectGroup(rc: RoutingContext)

    suspend fun getProjectStatusesInProjectGroup(rc: RoutingContext)

    suspend fun getProjectStatusesInProjectGroupByProject(rc: RoutingContext)

    suspend fun getGroupMembersWithMarks(rc: RoutingContext)

    suspend fun getGroups(rc: RoutingContext)
}

class AnalyticsFeatureImpl(
    private val projectRepo: ProjectRepo,
    private val projectGroupsRepo: ProjectsGroupRepo,
    private val branchesRepo: BranchesRepo,
    private val tokenUtil: GithubTokenUtil,
) : AnalyticsFeature {

    override suspend fun getGroups(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectGroupId = call.parameters["projectGroupId"]

            if (projectGroupId == null ||
                !projectGroupsRepo.checkIsCuratorGroupOwner(userId, projectGroupId)
            ) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val groups = projectGroupsRepo.getGroupProjects(projectGroupId)
                .flatMap { projectInGroup ->
                    projectRepo.getProject(projectInGroup.id, userId).let { project ->
                        projectInGroup.members.map {
                            it.group
                        }
                    }
                }
                .distinct()
            call.respondText(
                text = Json.encodeToString(mapOf("groups" to groups)),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun getGroupMembersWithMarks(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectGroupId = call.parameters["projectGroupId"]
            val group = call.parameters["group"]

            if (projectGroupId == null || group == null ||
                !projectGroupsRepo.checkIsCuratorGroupOwner(userId, projectGroupId)
            ) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val userIds = mutableListOf<ProjectInGroupMember>()
            val response = projectGroupsRepo.getGroupProjects(projectGroupId)
                .flatMap { projectInGroup ->
                    projectRepo.getProject(projectInGroup.id, userId).let { project ->
                        projectInGroup.members.filter {
                            it.group == group
                        }.map {
                            it to project.mark
                        }
                    }
                }
                .sortedByDescending { it.second }
                .filter {
                    if (!userIds.contains(it.first)) {
                        userIds.add(it.first)
                        true
                    }
                    else {
                        false
                    }
                }
                .map {
                    MemberWithMark(
                        firstName = it.first.firstName,
                        lastName = it.first.lastName,
                        secondName = it.first.secondName,
                        mark = it.second,
                    )
                }

            call.respondText(
                text = Json.encodeToString(response),
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun getProjectStatusesInProjectGroupByProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val projectId = call.parameters["projectId"]

            if (projectId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val groupId = projectGroupsRepo.getGroupId(projectId)

            if (!projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }


            val projectIds = projectGroupsRepo.getGroupProjects(groupId).map { it.id }
            val projectStatuses = projectIds.map {
                projectRepo.getProject(it, userId) to projectRepo.getProjectStatus(it)
            }
            var response = projectStatuses.map {
                ProjectWithStatus(
                    id = it.first.id,
                    title = it.first.meta.title,
                    status = it.second.name,
                )
            }
            call.respondText(
                text = Json.encodeToString(response),
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun getCommitsByProjectInProjectGroup(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val groupId = call.parameters["groupId"]

            val githubToken = tokenUtil.getGithubAccessToken(userId)
            if (githubToken == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }

            if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            var response = projectGroupsRepo.getGroupProjects(groupId)
                .map {
                    ProjectWithCommitsCount(
                        id = it.id,
                        title = it.title,
                        count = branchesRepo.getCommitsCount(it.id, githubToken).sumOf { it.commitsCount },
                    )
                }

            call.respondText(
                text = Json.encodeToString(response),
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun getCommitsByUsersInProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

            val projectId = call.parameters["projectId"]
            if (projectId == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val githubToken = tokenUtil.getGithubAccessToken(userId)
            if (githubToken == null) {
                call.respond(HttpStatusCode.TooEarly)
                return
            }
            val commiters = branchesRepo.getCommitsCount(projectId, githubToken)
            var response = commiters.associate {
                it.fullName to it.commitsCount
            }
            call.respondText(
                text = Json.encodeToString(response),
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun getProjectMarksInProjectGroup(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val groupId = call.parameters["groupId"]

            if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
                call.respond(HttpStatusCode.OK)
                return
            }

            @Serializable
            data class ProjectWithMark(
                val id: String,
                val title: String,
                val mark: Int?
            )

            var response = projectGroupsRepo.getGroupProjects(groupId)
                .map {
                    ProjectWithMark(
                        id = it.id,
                        title = it.title,
                        mark = it.mark,
                    )
                }

            call.respondText(
                text = Json.encodeToString(response),
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun getProjectStatusesInProjectGroup(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val groupId = call.parameters["groupId"]

            if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
                call.respond(HttpStatusCode.NotFound)
                return
            }
            val projectIds = projectGroupsRepo.getGroupProjects(groupId).map { it.id }
            val projectStatuses = projectIds.associate {
                projectRepo.getProjectTitle(it) to projectRepo.getProjectStatus(it).name
            }

            call.respondText(
                text = Json.encodeToString(projectStatuses),
                status = HttpStatusCode.OK
            )
        }
    }
}