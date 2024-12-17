package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class KardInSprintService(
    database: Database
) {

    object KardsInSprint: Table() {
        val id = integer("id").autoIncrement()
        val kardId = integer("kard_id")
            .references(KardService.Kards.id)
        val sprintId = integer("sprint_id")
            .references(SprintsService.Sprints.id)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(KardsInSprint)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(kardId_: String, sprintId_: String) = dbQuery {
        KardsInSprint.insert {
            it[kardId] = kardId_.toInt()
            it[sprintId] = sprintId_.toInt()
        }
    }

    suspend fun getSprintKardIds(sprintId_: String) = dbQuery {
        KardsInSprint.selectAll()
            .where { KardsInSprint.sprintId eq sprintId_.toInt()}
            .map {
                it[KardsInSprint.kardId]
            }
    }

    suspend fun onKardDeletion(kardId: String) = dbQuery {
        KardsInSprint.deleteWhere {
            KardsInSprint.kardId eq kardId.toInt()
        }
    }
}