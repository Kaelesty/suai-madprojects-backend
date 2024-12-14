package data.schemas

import entities.Message
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ProjectReposService(
    database: Database
) {

    object ProjectRepos: Table() {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)
        val link = varchar("link", length = 128)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ProjectRepos)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(projectId_: Int, link_: String) = dbQuery {
        ProjectRepos.insert {
            it[projectId] = projectId_
            it[link] = link_
        }
    }

    suspend fun getByProjectId(projectId_: Int) = dbQuery {
        ProjectRepos.selectAll()
            .where { ProjectRepos.projectId eq projectId_ }
            .map {
                it[ProjectRepos.id] to it[ProjectRepos.link]
            }
    }
}