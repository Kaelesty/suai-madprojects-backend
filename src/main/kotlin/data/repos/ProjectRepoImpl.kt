package data.repos

import data.schemas.ChatService
import data.schemas.ColumnsService
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
import entities.ChatType

class ProjectRepoImpl(
    private val userService: UserService,
    private val projectService: ProjectService,
    private val projectMembershipService: ProjectMembershipService,
    private val projectCuratorshipService: ProjectCuratorshipService,
    private val projectReposService: ProjectReposService,
    private val chatsService: ChatService,
    private val columnsService: ColumnsService,
) : ProjectRepo {

    override suspend fun updateProjectMeta(projectId: String, title: String?, desc: String?) {
        projectService.update(projectId.toInt(), title, desc)
    }

    override suspend fun getCuratorsList(): List<AvailableCurator> {
        return userService.getCurators()
    }

    override suspend fun createProject(
        request: CreateProjectRequest,
        userId: String,
    ): String {
        val newId = projectService.create(
            title_ = request.title,
            desc_ = request.desc,
            maxMembersCount_ = request.maxMembersCount,
            userId = userId.toInt(),
        )
        projectMembershipService.create(
            projectId_ = newId,
            userId_ = userId.toString()
        )
        projectCuratorshipService.create(
            projectId_ = newId,
            userId_ = request.curatorId.toString(),
            projectGroupId_ = request.projectGroupId
        )
        request.repoLinks.forEach {
            projectReposService.create(
                projectId_ = newId.toInt(),
                link_ = it
            )
        }


        chatsService.create(
            projectId_ = newId.toInt(),
            title_ = "Общий чат",
            chatType_ = ChatType.Public
        )
        chatsService.create(
            projectId_ = newId.toInt(),
            title_ = "Чат учасников",
            chatType_ = ChatType.MembersPrivate
        )
        chatsService.create(
            projectId_ = newId.toInt(),
            title_ = "Заметки преподавателя",
            chatType_ = ChatType.CuratorPrivate
        )

        columnsService.create(
            projectId_ = newId.toInt(),
            title_ = "Ожидание"
        )
        columnsService.create(
            projectId_ = newId.toInt(),
            title_ = "В процессе"
        )
        columnsService.create(
            projectId_ = newId.toInt(),
            title_ = "На проверке"
        )
        columnsService.create(
            projectId_ = newId.toInt(),
            title_ = "Готово"
        )

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