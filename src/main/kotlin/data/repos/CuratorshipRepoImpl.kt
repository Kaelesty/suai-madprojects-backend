package data.repos

import data.schemas.CommonUsersDataService
import data.schemas.ProjectCuratorshipService
import data.schemas.ProjectGroupService
import data.schemas.ProjectMembershipService
import data.schemas.ProjectService
import data.schemas.UnapprovedProjectService
import data.schemas.UserService
import domain.CuratorshipRepo
import domain.project.ProjectStatus
import domain.projectgroups.ProjectInGroupMember
import domain.projectgroups.ProjectInGroupView

class CuratorshipRepoImpl(
    private val curatorshipService: ProjectCuratorshipService,
    private val unapprovedProjectService: UnapprovedProjectService,
    private val projectService: ProjectService,
    private val projectMembershipService: ProjectMembershipService,
    private val userService: UserService,
    private val commonUsersDataService: CommonUsersDataService,
    private val projectGroupsService: ProjectGroupService,
) : CuratorshipRepo {

    private suspend fun getProjectInGroupViewById(projectId: Int): ProjectInGroupView {
        val project = projectService.getById(projectId)

        return ProjectInGroupView(
            id = projectId.toString(),
            title = project.title,
            members = projectMembershipService.getProjectUserIds(projectId.toString()).map {
                val user = userService.getById(it)
                if (user == null) {
                    null
                } else {
                    ProjectInGroupMember(
                        firstName = user.firstName,
                        secondName = user.secondName,
                        lastName = user.lastName,
                        group = commonUsersDataService.getByUser(it) ?: "null"
                    )
                }
            }.filterNotNull(),
            createDate = project.createDate,
            status = curatorshipService.getStatus(projectId),
            maxMembersCount = project.maxMembersCount,
            groupTitle = projectGroupsService
                .getGetById(
                    curatorshipService.getGroupId(
                        projectId.toString()
                    ).toInt()
                ).title,
            mark = curatorshipService.getMark(projectId)
        )
    }

    override suspend fun getUnmarkedProjects(curatorId: String): List<ProjectInGroupView> {
        return curatorshipService
            .getUnmarkedProjectIds(curatorId.toInt())
            .map { projectId ->
                getProjectInGroupViewById(projectId)
            }
    }

    override suspend fun getPendingProjects(curatorId: String): List<ProjectInGroupView> {
        return curatorshipService
            .getPendingProjectIds(curatorId.toInt())
            .map { projectId ->
                getProjectInGroupViewById(projectId)
            }
    }

    override suspend fun approveProject(curatorId: String, projectId: String) {
        curatorshipService.setStatus(
            projectId_ = projectId,
            userId_ = curatorId,
            status_ = ProjectStatus.Approved
        )
        curatorshipService.getGroupId(projectId).let {
            unapprovedProjectService.delete(it.toString())
        }

    }

    override suspend fun disapproveProject(curatorId: String, projectId: String, message: String) {
        curatorshipService.setStatus(
            projectId_ = projectId,
            userId_ = curatorId,
            status_ = ProjectStatus.Unapproved
        )
        curatorshipService.getGroupId(projectId).let {
            unapprovedProjectService.delete(it.toString())
            unapprovedProjectService.create(
                curatorshipId_ = it.toString(),
                reason_ = message
            )
        }
    }

    override suspend fun retrySubmission(projectId: String) {
        curatorshipService.getProjectCurator(projectId.toInt()).forEach {
            curatorshipService.setStatus(
                projectId_ = projectId,
                userId_ = it.toString(),
                status_ = ProjectStatus.Approved
            )
        }
        curatorshipService.getGroupId(projectId).let {
            unapprovedProjectService.delete(it.toString())
        }
    }
}