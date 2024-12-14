package data.schemas

import domain.project.ProjectMeta
import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ProjectService(
    database: Database
) {

    object Projects: Table("Projects_") {
        val id = integer("id").autoIncrement()
        val title = varchar("title", length = 25)
        val desc = varchar("desc", length = 1000)
        val maxMembersCount = integer("max_members_count")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Projects)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(title_: String, desc_: String, maxMembersCount_: Int) = dbQuery {
        val newId = Projects.insert {
            it[desc] = desc_
            it[title] = title_
            it[maxMembersCount] = maxMembersCount_
        }[Projects.id]
        return@dbQuery newId.toString()
    }

    suspend fun getById(projectId_: Int) = dbQuery {
        Projects.selectAll()
            .where { Projects.id eq projectId_}
            .map {
                ProjectMeta(
                    id = it[Projects.id].toString(),
                    title = it[Projects.title],
                    desc = it[Projects.desc],
                    maxMembersCount = it[Projects.maxMembersCount]
                )
            }
            .first()
    }
}