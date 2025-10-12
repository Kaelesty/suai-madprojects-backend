package app.plugins.analytics

import app.GithubTokenUtil
import app.plugins.Plugin
import domain.BranchesRepo
import domain.project.ProjectRepo
import domain.projectgroups.ProjectInGroupMember
import domain.projectgroups.ProjectsGroupRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnalyticsPlugin(
    private val projectRepo: ProjectRepo,
    private val projectGroupsRepo: ProjectsGroupRepo,
    private val branchesRepo: BranchesRepo,
    private val tokenUtil: GithubTokenUtil,
    private val excelWizard: ExcelWizard,
): Plugin {
    override fun setup(route: Route): Unit = with(route) {
        getGroups()
        getProjectStatusesInProjectGroup()
        getProjectStatusesInProjectGroupByProject()
        getUserCommits()
        getProjectGroupCommits()
        getGroupMarks()
        getProjectGroupMarks()
    }

    fun Route.getGroups() = get("/analytics/getGroups") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val projectGroupId = call.parameters["projectGroupId"]

        if (projectGroupId == null ||
            !projectGroupsRepo.checkIsCuratorGroupOwner(userId, projectGroupId)
        ) {
            call.respond(HttpStatusCode.Companion.NotFound)
            return@get
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
            text = Json.Default.encodeToString(mapOf("groups" to groups)),
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Companion.OK
        )
    }

    fun Route.getGroupMarks() = get("/analytics/groupMarks") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val projectGroupId = call.parameters["projectGroupId"]

        if (projectGroupId == null ||
            !projectGroupsRepo.checkIsCuratorGroupOwner(userId, projectGroupId)
        ) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        val groupName = projectGroupsRepo.getGroupTitle(projectGroupId)
        call.response.header(
            "Content-Disposition",
            "attachment; filename=\"${groupName}.xlsx\""
        )

        val userIds = mutableListOf<ProjectInGroupMember>()
        val data = projectGroupsRepo.getGroupProjects(projectGroupId)
            .flatMap { projectInGroup ->
                projectRepo.getProject(projectInGroup.id, userId).let { project ->
                    projectInGroup.members
                        .map {
                            it to project.mark
                        }
                }
            }
            .sortedByDescending { it.second }
            .filter {
                if (!userIds.contains(it.first)) {
                    userIds.add(it.first)
                    true
                } else {
                    false
                }
            }
            .map {
                MemberWithMark(
                    firstName = it.first.firstName,
                    lastName = it.first.lastName,
                    secondName = it.first.secondName,
                    mark = it.second,
                    group = it.first.group,
                )
            }

        call.respondBytes(
            bytes = excelWizard.excelify(data),
            contentType = ContentType.Application.OctetStream,
            status = HttpStatusCode.OK,
        )
    }

    fun Route.getProjectStatusesInProjectGroupByProject() = get("/analytics/projectStatusesByProjectId") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val projectId = call.parameters["projectId"]

        if (projectId == null) {
            call.respond(HttpStatusCode.Companion.NotFound)
            return@get
        }

        val groupId = projectGroupsRepo.getGroupId(projectId)

        if (!projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            call.respond(HttpStatusCode.Companion.NotFound)
            return@get
        }

        val projectIds = projectGroupsRepo.getGroupProjects(groupId).map { it.id }
        val projectStatuses = projectIds.map {
            projectRepo.getProject(it, userId) to projectRepo.getProjectStatus(it)
        }
        val response = projectStatuses.map {
            ProjectWithStatus(
                id = it.first.id,
                title = it.first.meta.title,
                status = it.second.name,
            )
        }
        call.respondText(
            text = Json.Default.encodeToString(response),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    fun Route.getProjectGroupCommits() = get("/analytics/projectGroupCommits") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val groupId = call.parameters["groupId"]

        val githubToken = tokenUtil.getGithubAccessToken(userId)
        if (githubToken == null) {
            call.respond(HttpStatusCode.Companion.TooEarly)
            return@get
        }

        if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            call.respond(HttpStatusCode.Companion.NotFound)
            return@get
        }

        val response = projectGroupsRepo.getGroupProjects(groupId)
            .map {
                ProjectWithCommitsCount(
                    id = it.id,
                    title = it.title,
                    count = branchesRepo.getCommitsCount(it.id, githubToken).sumOf { it.commitsCount },
                )
            }

        call.respondText(
            text = Json.Default.encodeToString(response),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    fun Route.getUserCommits() = get("/analytics/userCommits") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()

        val projectId = call.parameters["projectId"]
        if (projectId == null) {
            call.respond(HttpStatusCode.Companion.NotFound)
            return@get
        }

        val githubToken = tokenUtil.getGithubAccessToken(userId)
        if (githubToken == null) {
            call.respond(HttpStatusCode.Companion.TooEarly)
            return@get
        }
        val commiters = branchesRepo.getCommitsCount(projectId, githubToken)
        call.respondText(
            text = Json.encodeToString(commiters),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    fun Route.getProjectGroupMarks() = get("/analytics/projectGroupMarks") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val groupId = call.parameters["groupId"]

        if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            call.respond(HttpStatusCode.Companion.OK)
            return@get
        }

        val response = projectGroupsRepo.getGroupProjects(groupId)
            .map {
                ProjectWithMark(
                    id = it.id,
                    title = it.title,
                    mark = it.mark,
                )
            }

        call.respondText(
            text = Json.Default.encodeToString(response),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }

    fun Route.getProjectStatusesInProjectGroup() = get("/analytics/projectStatuses") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asString()
        val groupId = call.parameters["groupId"]

        if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
            call.respond(HttpStatusCode.Companion.NotFound)
            return@get
        }
        val projectIds = projectGroupsRepo.getGroupProjects(groupId).map { it.id }
        val projectStatuses = projectIds.map {
            ProjectTitleToId(
                title = projectRepo.getProjectTitle(it),
                statusName = projectRepo.getProjectStatus(it).name
            )
        }

        call.respondText(
            text = Json.Default.encodeToString(projectStatuses),
            status = HttpStatusCode.Companion.OK,
            contentType = ContentType.Application.Json,
        )
    }
}