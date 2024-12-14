package data.schemas

import domain.project.ProjectMeta
import domain.project.ProjectStatus
import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ProjectCuratorshipService(
    database: Database
) {

    object ProjectsCuratorship: Table() {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)
        val userId = integer("user_id")
            .references(UserService.Users.id)
        val status = enumerationByName<ProjectStatus>("status", 16)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ProjectsCuratorship)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(projectId_: String, userId_: String) = dbQuery {
        ProjectsCuratorship.insert {
            it[projectId] = projectId_.toInt()
            it[userId] = userId_.toInt()
            it[status] = ProjectStatus.Pending
        }
    }

    suspend fun getProjectCurator(projectId_: Int) = dbQuery {
        ProjectsCuratorship.selectAll()
            .where { ProjectsCuratorship.projectId eq projectId_ }
            .map {
                it[ProjectsCuratorship.userId]
            }
    }

    suspend fun setStatus(projectId_: String, userId_: String, status_: ProjectStatus) = dbQuery {
        ProjectsCuratorship.update(
            where = { (ProjectsCuratorship.projectId eq projectId_.toInt()) and (ProjectsCuratorship.userId eq userId_.toInt())  }
        ) {
            it[status] = status_
        }
    }
}