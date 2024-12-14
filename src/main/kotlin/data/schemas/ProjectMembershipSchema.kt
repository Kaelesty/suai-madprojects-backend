package data.schemas

import domain.project.ProjectMeta
import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ProjectMembershipService(
    database: Database
) {

    object ProjectsMembership: Table() {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)
        val userId = integer("user_id")
            .references(UserService.Users.id)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ProjectsMembership)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(projectId_: String, userId_: String) = dbQuery {
        ProjectsMembership.insert {
            it[projectId] = projectId_.toInt()
            it[userId] = userId_.toInt()
        }
    }

    suspend fun isUserInProject(userId_: String, projectId_: String) = dbQuery {
        ProjectsMembership.selectAll()
            .where { (ProjectsMembership.projectId eq projectId_.toInt()) and (ProjectsMembership.userId eq userId_.toInt())}
            .count() != 0L
    }

    suspend fun getUserProjectIds(userId_: Int) = dbQuery {
        ProjectsMembership.selectAll()
            .where { ProjectsMembership.userId eq userId_}
            .map {
                it[ProjectsMembership.projectId]
            }
    }
}