package data.database.repos

import data.database.executors.iExecutor
import data.database.executors.iExecutor.ProjectWithCardCount
import data.database.executors.lExecutor
import domain.database.iRepo
import domain.database.lRepo
import domain.profile.SharedProfile

class lRepoImpl(
    private val lExecutor: lExecutor
) : lRepo {
    override suspend fun getStudentsWithoutProjects() =
        lExecutor.getStudentsWithoutProjects()

    override suspend fun getProjectsWithoutCompletedSprints() =
        lExecutor.getProjectsWithoutCompletedSprints() ?: listOf()

    override suspend fun getAllUsers() =
        lExecutor.getAllUsers()

    override suspend fun getUnapprovedProjects() =
        lExecutor.getUnapprovedProjects() ?: listOf()

    override suspend fun getProjectsWithoutRepo() =
        lExecutor.getProjectsWithoutRepo()

    override suspend fun getLowRatingGroups() =
        lExecutor.getLowRatingGroups() ?: listOf()

    override suspend fun getOverdueSprints() =
        lExecutor.getOverdueSprints()

    override suspend fun getUsersWithoutGitHub() =
        lExecutor.getUsersWithoutGitHub() ?: listOf()

    override suspend fun approveProject(projectId: Int) =
        lExecutor.approveProject(projectId)

    override suspend fun deleteProject(projectId: Int) =
        lExecutor.deleteProject(projectId)
}