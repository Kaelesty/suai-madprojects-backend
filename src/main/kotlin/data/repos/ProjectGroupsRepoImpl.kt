package data.repos

import data.schemas.CommonUsersDataService
import data.schemas.ProjectCuratorshipService
import data.schemas.ProjectGroupService
import data.schemas.ProjectMembershipService
import data.schemas.ProjectService
import data.schemas.UserService
import domain.projectgroups.ProjectGroup
import domain.projectgroups.ProjectInGroupMember
import domain.projectgroups.ProjectInGroupView
import domain.projectgroups.ProjectsGroupRepo

class ProjectGroupsRepoImpl(
    private val projectGroupsService: ProjectGroupService,
    private val projectCuratorshipService: ProjectCuratorshipService,
    private val projectService: ProjectService,
    private val projectMembershipService: ProjectMembershipService,
    private val userService: UserService,
    private val commonUsersDataService: CommonUsersDataService,
) : ProjectsGroupRepo {

    override suspend fun getGroupId(projectId: String): String {
        return projectCuratorshipService.getGroupId(projectId).toString()
    }

    override suspend fun getGroupTitle(groupId: String): String {
        return projectGroupsService.getGetById(groupId.toInt()).title
    }

    override suspend fun createProjectsGroup(title: String, curatorId: String): ProjectGroup {
        return projectGroupsService.create(curatorId.toInt(), title)
    }

    override suspend fun getCuratorProjectGroups(curatorId: String): List<ProjectGroup> {
        return projectGroupsService.getCuratorProjectGroups(curatorId.toInt())
    }

    override suspend fun checkIsCuratorGroupOwner(curatorId: String, groupId: String): Boolean {
        return projectGroupsService.checkIsCuratorGroupOwner(curatorId.toInt(), groupId.toInt())
    }

    override suspend fun getGroupProjects(groupId: String): List<ProjectInGroupView> {
        val group = projectGroupsService.getGetById(groupId.toInt())
        val ids = projectCuratorshipService.getProjectGroupIds(groupId.toInt()).filter { !projectService.isProjectDeleted(it) }
        return ids
            .map { projectId ->

                val project = projectService.getById(projectId)

                ProjectInGroupView(
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
                    status = projectCuratorshipService.getStatus(projectId),
                    maxMembersCount = project.maxMembersCount,
                    groupTitle = group.title
                )
            }
    }
}