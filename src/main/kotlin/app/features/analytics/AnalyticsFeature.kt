package app.features.analytics

import domain.commits.CommitsRepo
import domain.project.ProjectRepo
import domain.projectgroups.ProjectsGroupRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext

interface AnalyticsFeature {

    suspend fun getCommitsByProjectInProjectGroup(rc: RoutingContext)

    suspend fun getCommitsByUsersInProject(rc: RoutingContext)

    suspend fun getProjectGradesInProjectGroup(rc: RoutingContext)

    suspend fun getProjectStatusesInProjectGroup(rc: RoutingContext)
}

class AnalyticsFeatureImpl(
    private val projectRepo: ProjectRepo,
    private val projectGroupsRepo: ProjectsGroupRepo,
    //private val commitsRepo: CommitsRepo,
): AnalyticsFeature {

    override suspend fun getCommitsByProjectInProjectGroup(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val groupId = call.parameters["groupId"]

            if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
                call.respond(HttpStatusCode.OK)
                return
            }

            val projectIds = projectGroupsRepo.getGroupProjects(groupId).map { it.id }
//            val commits = projectIds.map {
//                getProjectCommits(it)
//            }
//            var response = "project_name,commits\n"
//            commits.forEach {
//                response = response + "${it.projectName},${it.count}\n"
//            }
//            call.respondText(
//                text = response,
//                status = HttpStatusCode.OK
//            )

            // TODO UNFINISHED
        }
    }

    override suspend fun getCommitsByUsersInProject(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()

        }
    }

    override suspend fun getProjectGradesInProjectGroup(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("userId").asString()
            val groupId = call.parameters["groupId"]

            if (groupId == null || !projectGroupsRepo.checkIsCuratorGroupOwner(userId, groupId)) {
                call.respond(HttpStatusCode.OK)
                return
            }
            val projectIds = projectGroupsRepo.getGroupProjects(groupId).map { it.id }

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
            val projectStatuses = projectIds.map {
                projectRepo.getProjectTitle(it) to projectRepo.getProjectStatus(it)
            }
            var response = "project_name,status\n"
            projectStatuses.forEach {
                response = response + "${it.first},${it.second.name}\n"
            }
            call.respondText(
                text = response,
                status = HttpStatusCode.OK
            )
        }
    }

//    private suspend fun getProjectCommits(projectId: String): ProjectCommits {
//        return ProjectCommits(
//            projectId = projectId,
//            count = commitsRepo.getProjectCommits(projectId),
//            projectName = projectRepo.getProjectTitle(projectId)
//        )
//    }
}