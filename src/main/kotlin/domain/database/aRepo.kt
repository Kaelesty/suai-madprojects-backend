package domain.database

import data.database.executors.aExecutor
import domain.profile.SharedProfile

interface aRepo {
    // А.1 Найти пользователей проекта
    suspend fun getProjectUsers(projectId: Int): List<SharedProfile>

    // А.2 Проекты группы
    suspend fun getGroupProjects(groupId: Int): List<aExecutor.ProjectInfo>

    // А.3 Добавить проект в группу (Admin)
    suspend fun addProjectToGroup(
        projectId: Int,
        groupId: Int,
        curatorUserId: Int,
        initialStatus: String
    ): Boolean

    // А.4 Статусы всех проектов
    suspend fun getAllProjectStatuses(): List<aExecutor.ProjectStatusInfo>

    // А.5 Одобрить проект (Admin)
    suspend fun approveProject(projectId: Int): Boolean

    // А.6 Создать карточку в спринте (Admin)
    suspend fun createSprintKard(
        name: String,
        authorId: Int,
        desc: String,
        sprintId: Int,
        createTimeMillis: Long
    ): Boolean

    // А.7 Переместить карточку (Admin)
    suspend fun moveKard(kardId: Int, columnId: Int, newOrder: Int): Boolean

    // А.8 Оценки группы в CSV
    suspend fun getGroupMarksCsv(groupId: Int): aExecutor.GroupMarksCsv

    // А.9 Одобрить все проекты группы (Admin)
    suspend fun approveAllGroupProjects(groupId: Int): Boolean

    // А.10 Удалить пользователя из проекта (Admin)
    suspend fun removeUserFromProject(userId: Int, projectId: Int): Boolean
}