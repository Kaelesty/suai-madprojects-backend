package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ColumnsService(
    database: Database
) {

    object Columns: Table() {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
        val title = varchar("title", length = 25)
        val order = integer("order")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Columns)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(projectId_: Int, title_: String) = dbQuery {
        val lastOrder = Columns.selectAll()
            .where { Columns.projectId eq projectId_ }
            .sortedBy { it[Columns.order] }
            .map {
                it[Columns.order]
            }
            .lastOrNull()
        Columns.insert {
            it[projectId] = projectId_
            it[title] = title_
            it[order] = lastOrder?.plus(1) ?: 0
        }[Columns.id]
    }

    suspend fun getProjectId(id_: Int) = dbQuery {
        Columns.selectAll()
            .where { Columns.id eq id_ }
            .map {
                it[Columns.projectId]
            }
            .first()

    }

    suspend fun deleteColumn(id_: Int) = dbQuery {
        Columns.deleteWhere { id eq id_ }
    }

    suspend fun recalculateOrders(projectId_: Int) = dbQuery {
        val orders = getProjectColumns(projectId_)
            .sortedBy { it.order }
            .map { it.id }
        updateColumnsOrders(
            projectId_ = projectId_,
            newOrders = orders.mapIndexed { index, it ->
                NewColumnOrder(
                    rowId = it,
                    order = index
                )
            }
        )
    }

    suspend fun update(id_: Int, name_: String?) = dbQuery {
        Columns.update(
            where = { Columns.id eq id_ }
        ) {
            name_?.let { name_ -> it[title] = name_}
        }
    }

    suspend fun getProjectColumns(projectId_: Int) = dbQuery {
        Columns.selectAll()
            .where { Columns.projectId eq projectId_ }
            .map {
                ColumnEntity(
                    id = it[Columns.id],
                    projectId = it[Columns.projectId],
                    title = it[Columns.title],
                    order = it[Columns.order]
                )
            }
    }

    suspend fun updateColumnsOrders(projectId_: Int, newOrders: List<NewColumnOrder>) = dbQuery {
        val rows = getProjectColumns(projectId_)
        if (rows.size != newOrders.size) {
            throw IllegalStateException("Count on new orders a'nt match actual count of rows")
        }
        newOrders.forEach { new ->
            Columns.update(
                where = { Columns.id eq new.rowId }
            ) {
                it[order] = new.order
            }
        }

    }

    data class NewColumnOrder(
        val rowId: Int,
        val order: Int,
    )

    data class ColumnEntity(
        val id: Int,
        val projectId: Int,
        val title: String,
        val order: Int,
    )
}