package data.database.repos

import data.database.executors.aExecutor
import data.database.executors.aExecutor.GroupMarksCsv
import domain.database.aRepo

class aRepoImpl(
    private val aExecutor: aExecutor
) : aRepo {
    override suspend fun getProjectUsers(projectId: Int) =
        aExecutor.getProjectUsers(projectId) ?: emptyList()

    override suspend fun getGroupProjects(groupId: Int) =
        aExecutor.getGroupProjects(groupId) ?: emptyList()

    override suspend fun addProjectToGroup(
        projectId: Int,
        groupId: Int,
        curatorUserId: Int,
        initialStatus: String
    ) = aExecutor.addProjectToGroup(projectId, groupId, curatorUserId, initialStatus)

    override suspend fun getAllProjectStatuses() =
        aExecutor.getAllProjectStatuses() ?: emptyList()

    override suspend fun approveProject(projectId: Int) =
        aExecutor.approveProject(projectId)

    override suspend fun createSprintKard(
        name: String,
        authorId: Int,
        desc: String,
        sprintId: Int,
        createTimeMillis: Long
    ) = aExecutor.createSprintKard(name, authorId, desc, sprintId, createTimeMillis)

    override suspend fun moveKard(kardId: Int, columnId: Int, newOrder: Int) =
        aExecutor.moveKard(kardId, columnId, newOrder)

    override suspend fun getGroupMarksCsv(groupId: Int) =
        aExecutor.getGroupMarksCsv(groupId) ?: GroupMarksCsv("")

    override suspend fun approveAllGroupProjects(groupId: Int) =
        aExecutor.approveAllGroupProjects(groupId)

    override suspend fun removeUserFromProject(userId: Int, projectId: Int) =
        aExecutor.removeUserFromProject(userId, projectId)
}