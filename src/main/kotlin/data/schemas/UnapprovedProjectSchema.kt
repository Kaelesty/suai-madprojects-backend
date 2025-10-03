package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class UnapprovedProjectService(
    private val database: Database
) {

    object UnapprovedProjects: Table() {
        val id = integer("id").autoIncrement()
        val curatorshipId = integer("curatorship_id")
            .references(ProjectCuratorshipService.ProjectsCuratorship.id)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(UnapprovedProjects)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(db = database, context = Dispatchers.IO) { block() }

    suspend fun create(curatorshipId_: String) = dbQuery {
        UnapprovedProjects.insert {
            it[curatorshipId] = curatorshipId_.toInt()
        }
    }

    suspend fun delete(curatorshipId_: String) = dbQuery {
        UnapprovedProjects.deleteWhere {
            curatorshipId eq curatorshipId_.toInt()
        }
    }
}