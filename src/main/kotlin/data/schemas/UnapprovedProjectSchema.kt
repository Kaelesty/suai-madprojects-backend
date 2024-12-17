package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class UnapprovedProjectService(
    database: Database
) {

    object UnapprovedProjects: Table() {
        val id = integer("id").autoIncrement()
        val curatorshipId = integer("curatorship_id")
            .references(ProjectCuratorshipService.ProjectsCuratorship.id)
        val reason = varchar("reason", 1000)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(UnapprovedProjects)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(curatorshipId_: String, reason_: String) = dbQuery {
        UnapprovedProjects.insert {
            it[curatorshipId] = curatorshipId_.toInt()
            it[reason] = reason_
        }
    }

    suspend fun get(curatorshipId_: String) = dbQuery {
        UnapprovedProjects.selectAll()
            .where { UnapprovedProjects.curatorshipId eq curatorshipId_.toInt() }
            .map {
                it[UnapprovedProjects.reason]
            }
            .first()
    }

    suspend fun delete(curatorshipId_: String) = dbQuery {
        UnapprovedProjects.deleteWhere {
            curatorshipId eq curatorshipId_.toInt()
        }
    }
}