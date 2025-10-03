package data.database.executors

import data.database.DbConnector

import domain.profile.SharedProfile
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

class aExecutor(
    private val dbConnector: DbConnector
) {

    @Serializable
    data class ProjectStatusInfo(
        val projectId: Int,
        val title: String,
        val status: String?,
        val groupId: Int?
    )

    @Serializable
    data class GroupMarksCsv(
        val csvData: String
    )

    @Serializable
    data class ProjectInfo(
        val id: Int,
        val title: String,
        val creatorId: Int
    )

    // А.1 Найти пользователей, относящихся к проекту
    fun getProjectUsers(projectId: Int) = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT u.id, u.firstname, u.secondname, u.lastname
            FROM public.users u
            INNER JOIN public.projectsmembership pm ON u.id = pm.user_id
            WHERE pm.project_id = $projectId
        """.trimIndent()) { rs ->
            mutableListOf<SharedProfile>().apply {
                while (rs.next()) {
                    add(SharedProfile(
                        firstName = rs.getString("firstname"),
                        secondName = rs.getString("secondname"),
                        lastName = rs.getString("lastname")
                    ))
                }
            }
        } ?: emptyList()
    }

    // А.2 Найти проекты, относящиеся к группе проектов
    fun getGroupProjects(groupId: Int) = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT p.id, p.title, p.creator_id
            FROM public.projects_ p
            INNER JOIN public.projectscuratorship pc ON p.id = pc.project_id
            WHERE pc.project_group_id = $groupId
        """.trimIndent()) { rs ->
            mutableListOf<ProjectInfo>().apply {
                while (rs.next()) {
                    add(ProjectInfo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        creatorId = rs.getInt("creator_id")
                    ))
                }
            }
        } ?: emptyList()
    }

    // А.3 Добавить проект к группе проектов !Admin
    fun addProjectToGroup(
        projectId: Int,
        groupId: Int,
        curatorUserId: Int,
        initialStatus: String = "pending"
    ) = transaction(dbConnector.getAdminDatabase("editor_password")) {
        exec("""
            INSERT INTO public.projectscuratorship 
            (project_id, user_id, status, project_group_id)
            VALUES ($projectId, $curatorUserId, '$initialStatus', $groupId)
        """.trimIndent())
        true
    } ?: false

    // А.4 Получить статусы всех проектов
    fun getAllProjectStatuses() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT p.id, p.title, pc.status, pc.project_group_id
            FROM public.projects_ p
            LEFT JOIN public.projectscuratorship pc ON p.id = pc.project_id
        """.trimIndent()) { rs ->
            mutableListOf<ProjectStatusInfo>().apply {
                while (rs.next()) {
                    add(ProjectStatusInfo(
                        projectId = rs.getInt("id"),
                        title = rs.getString("title"),
                        status = rs.getString("status"),
                        groupId = rs.getInt("project_group_id")
                    ))
                }
            }
        } ?: emptyList()
    }

    // А.5 Одобрить проект
    fun approveProject(projectId: Int) = transaction(dbConnector.getAdminDatabase("editor_password")) {
        exec("""
            DELETE FROM public.unapprovedprojects 
            WHERE curatorship_id = $projectId
        """.trimIndent())
        true
    } ?: false

    // А.6 Создание новой карточки в спринте !Admin
    fun createSprintKard(
        name: String,
        authorId: Int,
        desc: String,
        sprintId: Int,
        createTimeMillis: Long = System.currentTimeMillis()
    ) = transaction(dbConnector.getAdminDatabase("editor_password")) {
        val kardId = exec("""
            INSERT INTO public.kards 
            (name, "authorId", desc, create_time_millis, update_time_millis)
            VALUES ('$name', $authorId, '$desc', $createTimeMillis, $createTimeMillis)
            RETURNING id
        """.trimIndent()) { rs -> if (rs.next()) rs.getInt(1) else null }

        kardId?.let {
            exec("""
                INSERT INTO public.kardsinsprint (kard_id, sprint_id)
                VALUES ($it, $sprintId)
            """.trimIndent())
            true
        } ?: false
    } ?: false

    // А.7 Передвижение карточки в спринте !Admin
    fun moveKard(kardId: Int, columnId: Int, newOrder: Int) = transaction(dbConnector.getAdminDatabase("editor_password")) {
        exec("""
            UPDATE public.kardorders 
            SET "order" = $newOrder 
            WHERE kard_id = $kardId AND column_id = $columnId
        """.trimIndent())
        true
    } ?: false

    // А.8 Получить оценки в группе проектов в формате csv
    fun getGroupMarksCsv(groupId: Int) = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT string_agg(
                CONCAT_WS(',', project_id, user_id, mark), 
                E'\n'
            ) AS csv
            FROM public.projectscuratorship
            WHERE project_group_id = $groupId
        """.trimIndent()) { rs ->
            if (rs.next()) GroupMarksCsv(rs.getString("csv") ?: "")
            else GroupMarksCsv("")
        }
    }

    // А.9 Одобрить все проекты в группе !Admin
    fun approveAllGroupProjects(groupId: Int) = transaction(dbConnector.getAdminDatabase("editor_password")) {
        exec("""
            DELETE FROM public.unapprovedprojects 
            WHERE curatorship_id IN (
                SELECT id FROM public.projectscuratorship 
                WHERE project_group_id = $groupId
            )
        """.trimIndent())
        true
    } ?: false

    // А.10 Удалить пользователя из проекта !Admin
    fun removeUserFromProject(userId: Int, projectId: Int) = transaction(dbConnector.getAdminDatabase("editor_password")) {
        exec("""
            DELETE FROM public.projectsmembership 
            WHERE user_id = $userId AND project_id = $projectId
        """.trimIndent())
        true
    } ?: false
}