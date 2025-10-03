package domain.database

import data.database.executors.lExecutor
import domain.profile.SharedProfile

interface lRepo {
    // Л.1 Найти всех студентов без проектов
    suspend fun getStudentsWithoutProjects(): List<SharedProfile>

    // Л.2 Проекты без завершенных спринтов
    suspend fun getProjectsWithoutCompletedSprints(): List<lExecutor.ProjectInfo>

    // Л.3 Получить всех пользователей
    suspend fun getAllUsers(): List<SharedProfile>

    // Л.4 Неодобренные проекты
    suspend fun getUnapprovedProjects(): List<lExecutor.ProjectInfo>

    // Л.5 Проекты без репозитория
    suspend fun getProjectsWithoutRepo(): List<lExecutor.ProjectWithRepoStatus>

    // Л.6 Группы с низкой средней оценкой
    suspend fun getLowRatingGroups(): List<lExecutor.GroupInfo>

    // Л.7 Просроченные спринты
    suspend fun getOverdueSprints(): List<lExecutor.SprintInfo>

    // Л.8 Пользователи без GitHub
    suspend fun getUsersWithoutGitHub(): List<SharedProfile>

    // Л.9 Одобрить проект (Admin)
    suspend fun approveProject(projectId: Int): Boolean

    // Л.10 Удалить проект (Admin)
    suspend fun deleteProject(projectId: Int): Boolean
}