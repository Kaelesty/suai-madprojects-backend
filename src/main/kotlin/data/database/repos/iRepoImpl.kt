package data.database.repos

import data.database.executors.iExecutor
import data.database.executors.iExecutor.ProjectWithCardCount
import domain.database.iRepo
import domain.profile.SharedProfile

class iRepoImpl(
    private val iExecutor: iExecutor
): iRepo {

    override suspend fun getUnassignedCurators() = iExecutor.getUnassignedCurators()

    override suspend fun getPendingApprovalProjects(): List<iExecutor.ProjectInfo> =
        iExecutor.getPendingApprovalProjects()

    override suspend fun getProjectsWithExpiredSprints(): List<iExecutor.ProjectInfo> =
        iExecutor.getProjectsWithExpiredSprints()

    override suspend fun getGroupsWithAllProjectsCompleted(): List<iExecutor.ProjectGroupInfo> =
        iExecutor.getGroupsWithAllProjectsCompleted()

    override suspend fun getProjectsWithLowMarks(): List<iExecutor.ProjectInfo> =
        iExecutor.getProjectsWithLowMarks()

    override suspend fun getGroupsWithLowAverage(): List<iExecutor.ProjectGroupInfo> =
        iExecutor.getGroupsWithLowAverage()

    override suspend fun getProjectsWithAboveAverageCards(): List<ProjectWithCardCount> =
        iExecutor.getProjectsWithAboveAverageCards()

    override suspend fun getExcellentProjects(): List<iExecutor.ProjectInfo> =
        iExecutor.getExcellentProjects()

    override suspend fun getUsersWithoutChatParticipation(): List<SharedProfile> =
        iExecutor.getUsersWithoutChatParticipation()

    override suspend fun getMostActiveChats(limit: Int): List<iExecutor.ChatInfo> =
        iExecutor.getMostActiveChats(limit)
}