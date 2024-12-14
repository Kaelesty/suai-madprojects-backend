package domain.project

import domain.profile.ProfileProject

interface ProjectRepo {

    suspend fun getCuratorsList(): List<AvailableCurator>

    suspend fun createProject(request: CreateProjectRequest, userId: String): String

    suspend fun getUserProjects(userId: String): List<ProfileProject>

    suspend fun checkUserInProject(userId: String, projectId: String): Boolean
}