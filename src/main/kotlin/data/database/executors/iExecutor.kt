package data.database.executors

import data.database.DbConnector
import domain.profile.SharedProfile
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

class iExecutor(
    private val dbConnector: DbConnector
) {
    // Data классы для возвращаемых сущностей
    @Serializable data class ProjectInfo(val id: Int, val title: String, val creatorId: Int)
    @Serializable data class ProjectGroupInfo(val id: Int, val title: String, val curatorId: Int)
    @Serializable data class ChatInfo(
        val id: Int,
        val chatTitle: String,
        val projectTitle: String,
        val messageCount: Int
    )

    @Serializable
    data class ProjectWithCardCount(
        val id: Int,
        val title: String,
        val creatorId: Int,
        val cardCount: Int
    )

    fun getUnassignedCurators() = transaction(dbConnector.getCommonDatabase()) {
        exec("" +
                "SELECT \n" +
                "    u.id AS curator_id,\n" +
                "    u.username,\n" +
                "    u.lastname,\n" +
                "    u.firstname,\n" +
                "    u.secondname\n" +
                "FROM \n" +
                "    public.users u\n" +
                "INNER JOIN \n" +
                "    public.curatorsdata cd ON u.id = cd.user_id\n" +
                "LEFT JOIN \n" +
                "    public.projectscuratorship pc ON u.id = pc.user_id\n" +
                "WHERE \n" +
                "    pc.id IS NULL;" +
                "") { rs ->
            val result = mutableListOf<SharedProfile>()
            if (rs.next()) {
                result.add(
                    SharedProfile(
                        firstName = rs.getString("firstname"),
                        secondName = rs.getString("secondname"),
                        lastName = rs.getString("lastname")
                    )
                )
            }
            result.toList()
        }
    } ?: listOf()

    // 4.2. Проекты ожидающие одобрения
    fun getPendingApprovalProjects() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT p.id, p.title, p.creator_id
            FROM public.projects_ p
            INNER JOIN public.unapprovedprojects up ON p.id = up.curatorship_id
        """.trimIndent()) { rs ->
            val result = mutableListOf<ProjectInfo>()
            while (rs.next()) {
                result.add(
                    ProjectInfo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        creatorId = rs.getInt("creator_id")
                    )
                )
            }
            result.toList()
        }
    } ?: emptyList()

    // 4.3. Проекты с истекшим сроком спринта
    fun getProjectsWithExpiredSprints() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT DISTINCT p.id, p.title, p.creator_id
            FROM public.projects_ p
            INNER JOIN public.sprints_ s ON p.id = s.project_id
            WHERE TO_DATE(s."supposedEndDate", 'DD.MM.YYYY') < CURRENT_DATE
            AND s."actualEndDate" IS NULL
        """.trimIndent()) { rs ->
            val result = mutableListOf<ProjectInfo>()
            while (rs.next()) {
                result.add(
                    ProjectInfo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        creatorId = rs.getInt("creator_id")
                    )
                )
            }
            result.toList()
        }
    } ?: emptyList()

    // 4.4. Группы с завершенными проектами
    fun getGroupsWithAllProjectsCompleted() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT pg.id, pg.title, pg.curator_id
            FROM public.projectsgroup pg
            WHERE NOT EXISTS (
                SELECT 1
                FROM public.projectscuratorship pc
                WHERE pc.project_group_id = pg.id
                AND pc.status != 'completed'
            )
        """.trimIndent()) { rs ->
            val result = mutableListOf<ProjectGroupInfo>()
            while (rs.next()) {
                result.add(
                    ProjectGroupInfo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        curatorId = rs.getInt("curator_id")
                    )
                )
            }
            result.toList()
        }
    } ?: emptyList()

    // 4.5. Проекты с оценкой ниже 4
    fun getProjectsWithLowMarks() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT DISTINCT p.id, p.title, p.creator_id
            FROM public.projects_ p
            INNER JOIN public.projectscuratorship pc ON p.id = pc.project_id
            WHERE pc.mark < 4
        """.trimIndent()) { rs ->
            val result = mutableListOf<ProjectInfo>()
            while (rs.next()) {
                result.add(
                    ProjectInfo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        creatorId = rs.getInt("creator_id")
                    )
                )
            }
            result.toList()
        }
    } ?: emptyList()

    // 4.6. Группы с низкой средней оценкой
    fun getGroupsWithLowAverage() = transaction(dbConnector.getCommonDatabase()) {
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
            val result = mutableListOf<ProjectGroupInfo>()
            while (rs.next()) {
                result.add(
                    ProjectGroupInfo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        curatorId = rs.getInt("curator_id")
                    )
                )
            }
            result.toList()
        }
    } ?: emptyList()

    // 4.7. Проекты с количеством коммитов выше среднего
    fun getProjectsWithAboveAverageCards() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
        SELECT 
            p.id,
            p.title,
            p.creator_id,
            COUNT(DISTINCT k.id) AS card_count
        FROM 
            public.projects_ p
        LEFT JOIN 
            public."Columns" c ON p.id = c.project_id
        LEFT JOIN 
            public.kardorders ko ON c.id = ko.column_id
        LEFT JOIN 
            public.kards k ON ko.kard_id = k.id
        GROUP BY 
            p.id
        HAVING 
            COUNT(DISTINCT k.id) > (
                SELECT AVG(cnt) 
                FROM (
                    SELECT COUNT(DISTINCT ko2.kard_id) AS cnt 
                    FROM public.kardorders ko2
                    JOIN public."Columns" c2 ON ko2.column_id = c2.id
                    GROUP BY c2.project_id
                ) AS sub
            )
        ORDER BY 
            card_count DESC
    """.trimIndent()) { rs ->
            val result = mutableListOf<ProjectWithCardCount>()
            while (rs.next()) {
                result.add(
                    ProjectWithCardCount(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        creatorId = rs.getInt("creator_id"),
                        cardCount = rs.getInt("card_count")
                    )
                )
            }
            result
        } ?: emptyList()
    }

    // 4.8. Проекты с отличной оценкой
    fun getExcellentProjects() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT DISTINCT p.id, p.title, p.creator_id
            FROM public.projects_ p
            INNER JOIN public.projectscuratorship pc ON p.id = pc.project_id
            WHERE pc.mark = 5
        """.trimIndent()) { rs ->
            val result = mutableListOf<ProjectInfo>()
            while (rs.next()) {
                result.add(
                    ProjectInfo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        creatorId = rs.getInt("creator_id")
                    )
                )
            }
            result.toList()
        }
    } ?: emptyList()

    // 4.9. Пользователи не участвовавшие в чатах
    fun getUsersWithoutChatParticipation() = transaction(dbConnector.getCommonDatabase()) {
        exec("""
            SELECT u.id, u.username, u.lastname, u.firstname, u.secondname
            FROM public.users u
            INNER JOIN public.projectsmembership pm ON u.id = pm.user_id
            LEFT JOIN public.messages m ON u.id = m.sender_id
            WHERE m.id IS NULL
        """.trimIndent()) { rs ->
            val result = mutableListOf<SharedProfile>()
            while (rs.next()) {
                result.add(
                    SharedProfile(
                        firstName = rs.getString("firstname"),
                        secondName = rs.getString("secondname"),
                        lastName = rs.getString("lastname")
                    )
                )
            }
            result.toList()
        }
    } ?: emptyList()

    // 4.10. Чаты с наибольшим количеством сообщений
    fun getMostActiveChats(limit: Int = 10) = transaction(dbConnector.getCommonDatabase()) {
        exec("""
        SELECT 
            c.id, 
            c.title AS chat_title, 
            p.title AS project_title,
            COUNT(m.id) as message_count
        FROM public.chats c
        INNER JOIN public.messages m ON c.id = m.chat_id
        INNER JOIN public.projects_ p ON c.project_id = p.id
        GROUP BY c.id, c.title, p.title
        ORDER BY message_count DESC
        LIMIT $limit
    """.trimIndent()) { rs ->
            val result = mutableListOf<ChatInfo>()
            while (rs.next()) {
                result.add(
                    ChatInfo(
                        id = rs.getInt("id"),
                        chatTitle = rs.getString("chat_title"),
                        projectTitle = rs.getString("project_title"),
                        messageCount = rs.getInt("message_count")
                    )
                )
            }
            result.toList()
        }
    } ?: emptyList()
}