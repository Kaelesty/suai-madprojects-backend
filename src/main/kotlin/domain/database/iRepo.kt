package domain.database

import data.database.executors.iExecutor
import data.database.executors.iExecutor.ProjectWithCardCount
import domain.profile.SharedProfile

interface iRepo {
    // 4.1. Получить список кураторов без назначенных проектов
    suspend fun getUnassignedCurators(): List<SharedProfile>

    // 4.2. Получить список проектов, которые ожидают одобрения
    suspend fun getPendingApprovalProjects(): List<iExecutor.ProjectInfo>

    // 4.3. Найти проекты, у которых истек срок выполнения спринта
    suspend fun getProjectsWithExpiredSprints(): List<iExecutor.ProjectInfo>

    // 4.4. Получить список групп проектов, где все проекты уже завершены
    suspend fun getGroupsWithAllProjectsCompleted(): List<iExecutor.ProjectGroupInfo>

    // 4.5. Найти проекты, у которых оценка ниже 4
    suspend fun getProjectsWithLowMarks(): List<iExecutor.ProjectInfo>

    // 4.6. Получить группы проектов, где средняя оценка проектов ниже 4
    suspend fun getGroupsWithLowAverage(): List<iExecutor.ProjectGroupInfo>

    // 4.7. Найти проекты, в которых количество коммитов превышает среднее значение
    suspend fun getProjectsWithAboveAverageCards(): List<ProjectWithCardCount>

    // 4.8. Найти проекты, которые получили оценку отлично
    suspend fun getExcellentProjects(): List<iExecutor.ProjectInfo>

    // 4.9. Получить список пользователей, которые не участвовали в чате проекта
    suspend fun getUsersWithoutChatParticipation(): List<SharedProfile>

    // 4.10. Найти чаты, в которых больше всего сообщений
    suspend fun getMostActiveChats(limit: Int = 10): List<iExecutor.ChatInfo>
}