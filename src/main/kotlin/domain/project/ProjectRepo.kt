package domain.project

import domain.profile.ProfileProject

interface ProjectRepo {

    suspend fun getCuratorsList(): List<AvailableCurator>

    suspend fun createProject(request: CreateProjectRequest, userId: String): String

    suspend fun getUserProjects(userId: String): List<ProfileProject>

    suspend fun checkUserInProject(userId: String, projectId: String): Boolean

    suspend fun getProject(projectId: String, userId: String): Project

    suspend fun updateProjectMeta(projectId: String, title: String?, desc: String?)

    suspend fun checkUserIsCreator(userId: String, projectId: String): Boolean

    suspend fun deleteProject(projectId: String)

    suspend fun removeProjectMember(userId: String, projectId: String)
}