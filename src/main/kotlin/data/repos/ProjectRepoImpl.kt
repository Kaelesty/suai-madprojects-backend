package data.repos

import data.schemas.ProjectCuratorshipService
import data.schemas.ProjectMembershipService
import data.schemas.ProjectReposService
import data.schemas.ProjectService
import data.schemas.UserService
import domain.profile.ProfileProject
import domain.project.AvailableCurator
import domain.project.CreateProjectRequest
import domain.project.Project
import domain.project.ProjectMember
import domain.project.ProjectRepo
import domain.project.ProjectRepository

class ProjectRepoImpl(
    private val userService: UserService,
    private val projectService: ProjectService,
    private val projectMembershipService: ProjectMembershipService,
    private val projectCuratorshipService: ProjectCuratorshipService,
    private val projectReposService: ProjectReposService
) : ProjectRepo {

    override suspend fun updateProjectMeta(projectId: String, title: String?, desc: String?) {
        projectService.update(projectId.toInt(), title, desc)
    }

    override suspend fun getCuratorsList(): List<AvailableCurator> {
        return userService.getCurators()
    }

    override suspend fun createProject(request: CreateProjectRequest, userId: String): String {
        val newId = projectService.create(
            title_ = request.title,
            desc_ = request.desc,
            maxMembersCount_ = request.maxMembersCount
        )
        projectMembershipService.create(
            projectId_ = newId,
            userId_ = userId.toString()
        )
        projectCuratorshipService.create(
            projectId_ = newId,
            userId_ = request.curatorId.toString()
        )
        request.repoLinks.forEach {
            projectReposService.create(
                projectId_ = newId.toInt(),
                link_ = it
            )
        }
        return newId
    }

    override suspend fun getUserProjects(userId: String): List<ProfileProject> {
        val projectsId = projectMembershipService.getUserProjectIds(userId.toInt())
        return projectsId.map {
            projectService.getById(it).let {
                ProfileProject(
                    id = it.id,
                    title = it.title
                )
            }
        }
    }

    override suspend fun checkUserInProject(userId: String, projectId: String): Boolean {
        return projectMembershipService.isUserInProject(userId, projectId)
                || projectCuratorshipService.getProjectCurator(
            projectId.toInt()
        ).contains(
            userId.toInt()
        )
    }

    override suspend fun getProject(projectId: String, userId: String): Project {
        return Project(
            id = projectId,
            meta = projectService.getById(projectId.toInt()),
            members = projectMembershipService.getProjectUserIds(projectId).map {
                userService.getById(it)?.let {
                    ProjectMember(
                        id = it.id,
                        lastName = it.lastName,
                        firstName = it.firstName,
                        secondName = it.secondName,
                    )
                }
            }.filterNotNull(),
            repos = projectReposService.getByProjectId(projectId.toInt())
                .map {
                    ProjectRepository(
                        id = it.first.toString(),
                        link = it.second,
                        title = it.second.split("/").last()
                    )
                },
            isCreator = projectService.getCreatorId(projectId.toInt()).toString() == userId
        )
    }
}