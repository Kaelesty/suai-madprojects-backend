package data.schemas

import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class InvitesService(
    database: Database
) {

    object Invites: Table() {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)
        val invite = varchar("invite", 128)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Invites)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(projectId_: Int, invite_: String) = dbQuery {
        val newId = Invites.insert {
            it[projectId] = projectId_
            it[invite] = invite_
        }[Invites.id]
        return@dbQuery newId
    }

    suspend fun getByProjectId(projectId_: Int) = dbQuery {
        Invites.selectAll()
            .where { Invites.projectId eq projectId_ }
            .map {
               it[Invites.invite]
            }
            .firstOrNull()
    }

    suspend fun refreshByProjectId(projectId_: Int, invite_: String) = dbQuery {
        Invites.update(
            where = { Invites.projectId eq projectId_ }
        ) {
            it[invite] = invite_
        }
    }

    suspend fun checkIsUnique(invite_: String) = dbQuery {
        Invites.selectAll()
            .where { Invites.invite eq invite_ }
            .count() == 0L
    }

    suspend fun getProjectByInvite(invite_: String) = dbQuery {
        Invites.selectAll()
            .where { Invites.invite eq invite_ }
            .map {
                it[Invites.projectId]
            }
            .firstOrNull()
    }
}