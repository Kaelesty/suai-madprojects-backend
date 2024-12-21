package domain

import domain.projectgroups.ProjectInGroupView

interface CuratorshipRepo {

    suspend fun approveProject(curatorId: String, projectId: String)

    suspend fun disapproveProject(curatorId: String, projectId: String, message: String)

    suspend fun retrySubmission(projectId: String)

    suspend fun getPendingProjects(curatorId: String): List<ProjectInGroupView>

    suspend fun getUnmarkedProjects(curatorId: String): List<ProjectInGroupView>
}