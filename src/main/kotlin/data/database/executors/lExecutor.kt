package data.database.executors

import org.jetbrains.exposed.sql.Database

import data.database.DbConnector
import domain.profile.SharedProfile
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

class lExecutor(
    private val dbConnector: DbConnector
) {
    @Serializable
    data class ProjectInfo(val id: Int, val title: String, val creatorId: Int)

    @Serializable
    data class GroupInfo(val id: Int, val title: String, val curatorId: Int)

    @Serializable
    data class SprintInfo(
        val id: Int,
        val title: String,
        val projectId: Int,
        val supposedEndDate: String,
        val actualEndDate: String?
    )

    @Serializable
    data class ProjectWithRepoStatus(val id: Int, val title: String, val hasRepo: Boolean)

    // Л.1 Найти всех студентов, которые не состоят ни в одном проекте
    fun getStudentsWithoutProjects() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT u.id, u.username, u.lastname, u.firstname, u.secondname
            FROM public.users u
            WHERE u."userType" = 'Common'
            AND NOT EXISTS (
                SELECT 1 
                FROM public.projectsmembership pm 
                WHERE pm.user_id = u.id
            )
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

    // Л.2 Проекты без завершенных спринтов
    fun getProjectsWithoutCompletedSprints() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT p.id, p.title, p.creator_id
            FROM public.projects_ p
            WHERE NOT EXISTS (
                SELECT 1 
                FROM public.sprints_ s 
                WHERE s.project_id = p.id 
                AND s."actualEndDate" IS NOT NULL
            )
        """.trimIndent()) { rs ->
            parseProjectInfo(rs)
        }
    }

    // Л.3 Получить всех пользователей
    fun getAllUsers() = transaction(dbConnector.getCommonDatabase()) {
        exec("SELECT * FROM public.users") { rs ->
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

    // Л.4 Неодобренные проекты
    fun getUnapprovedProjects() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT p.id, p.title, p.creator_id
            FROM public.projects_ p
            INNER JOIN public.unapprovedprojects up ON p.id = up.curatorship_id
        """.trimIndent()) { rs ->
            parseProjectInfo(rs)
        }
    }

    // Л.5 Проекты без репозитория GitHub
    fun getProjectsWithoutRepo() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT p.id, p.title, pr.link IS NULL as has_repo
            FROM public.projects_ p
            LEFT JOIN public.projectrepos pr ON p.id = pr.project_id
            WHERE pr.id IS NULL
        """.trimIndent()) { rs ->
            mutableListOf<ProjectWithRepoStatus>().apply {
                while (rs.next()) {
                    add(ProjectWithRepoStatus(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        hasRepo = rs.getBoolean("has_repo")
                    ))
                }
            }
        } ?: emptyList()
    }

    // Л.6 Группы с низкой средней оценкой (аналогично примеру 4.6)
    fun getLowRatingGroups() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT pg.id, pg.title, pg.curator_id
            FROM public.projectsgroup pg
            INNER JOIN (
                SELECT project_group_id, AVG(mark) as avg_mark
                FROM public.projectscuratorship
                GROUP BY project_group_id
                HAVING AVG(mark) < 4
            ) pc ON pg.id = pc.project_group_id
        """.trimIndent()) { rs ->
            parseGroupInfo(rs)
        }
    }

    // Л.7 Просроченные спринты
    fun getOverdueSprints() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT id, title, project_id, "supposedEndDate", "actualEndDate"
            FROM public.sprints_
            WHERE "actualEndDate" IS NULL
            AND TO_DATE("supposedEndDate", 'DD.MM.YYYY') < CURRENT_DATE
        """.trimIndent()) { rs ->
            mutableListOf<SprintInfo>().apply {
                while (rs.next()) {
                    add(SprintInfo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        projectId = rs.getInt("project_id"),
                        supposedEndDate = rs.getString("supposedEndDate"),
                        actualEndDate = rs.getString("actualEndDate")
                    ))
                }
            }
        } ?: emptyList()
    }

    // Л.8 Пользователи без GitHub
    fun getUsersWithoutGitHub() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT u.id, u.username, u.lastname, u.firstname, u.secondname
            FROM public.users u
            LEFT JOIN public.githubtokens gt ON u.id = gt.user_id
            WHERE gt.id IS NULL
        """.trimIndent()) { rs ->
            parseSharedProfiles(rs)
        }
    }

    // Л.9 Одобрить проект (Admin)
    fun approveProject(projectId: Int) = transaction(dbConnector.getAdminDatabase("editor_password")) {
        exec("""
            DELETE FROM public.unapprovedprojects
            WHERE curatorship_id = $projectId
        """.trimIndent())
        true
    } ?: false

    // Л.10 Удалить проект (Admin)
    fun deleteProject(projectId: Int) = transaction(dbConnector.getAdminDatabase("editor_password")) {
        exec("""
            UPDATE public.projects_ 
            SET is_deleted = true 
            WHERE id = $projectId
        """.trimIndent())
        true
    } ?: false

    private fun parseProjectInfo(rs: java.sql.ResultSet): List<ProjectInfo> {
        val result = mutableListOf<ProjectInfo>()
        while (rs.next()) {
            result.add(ProjectInfo(
                id = rs.getInt("id"),
                title = rs.getString("title"),
                creatorId = rs.getInt("creator_id")
            ))
        }
        return result
    }

    private fun parseGroupInfo(rs: java.sql.ResultSet): List<GroupInfo> {
        val result = mutableListOf<GroupInfo>()
        while (rs.next()) {
            result.add(GroupInfo(
                id = rs.getInt("id"),
                title = rs.getString("title"),
                curatorId = rs.getInt("curator_id")
            ))
        }
        return result
    }

    private fun parseSharedProfiles(rs: java.sql.ResultSet): List<SharedProfile> {
        val result = mutableListOf<SharedProfile>()
        while (rs.next()) {
            result.add(SharedProfile(
                firstName = rs.getString("firstname"),
                secondName = rs.getString("secondname"),
                lastName = rs.getString("lastname")
            ))
        }
        return result
    }
}