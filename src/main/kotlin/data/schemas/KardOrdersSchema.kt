package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class KardOrdersService(
    private val database: Database
) {

    object KardOrders : Table() {
        val id = integer("id").autoIncrement()
        val kardId = integer("kard_id")
            .references(KardService.Kards.id)
        val columnId = integer("column_id")
            .references(ColumnsService.Columns.id)
        val order = integer("order")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(KardOrders)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(kardId_: Int, columnId_: Int) = dbQuery {

        val lastOrder = dbQuery {
            KardOrders.selectAll()
                .where { KardOrders.columnId eq columnId_ }
                .map {
                    it[KardOrders.order]
                }
                .maxOrNull()
        }

        KardOrders.insert {
            it[kardId] = kardId_
            it[columnId] = columnId_
            it[order] = lastOrder?.plus(1) ?: 0
        }
    }

    suspend fun delete(kardId_: Int) = dbQuery {
        KardOrders.deleteWhere { kardId eq kardId_}
    }

    suspend fun getKardColumns(kardId_: Int) = dbQuery {
        KardOrders.selectAll()
            .where { KardOrders.kardId eq kardId_ }
            .map {
                it[KardOrders.columnId]
            }
    }

    suspend fun getColumnKards(columnId_: Int) = dbQuery {
        KardOrders.selectAll()
            .where { KardOrders.columnId eq columnId_ }
            .map {
                KardOrderEntity(
                    id = it[KardOrders.id],
                    kardId = it[KardOrders.kardId],
                    columnId = it[KardOrders.columnId],
                    order = it[KardOrders.order]
                )
            }
    }

    suspend fun updateKardOrders(columnId_: Int, orders: List<NewKardOrder>) = dbQuery {
        val kards = getColumnKards(columnId_)
        if (kards.size != orders.size) {
            throw IllegalStateException("Count on new orders a'nt match actual count of kards")
        }
        orders.forEach { new ->
            KardOrders.update(
                where = { KardOrders.kardId eq new.kardId }
            ) {
                it[order] = new.order
            }
        }
    }

    suspend fun delFromColumnByKardId(kardId_: Int, columnId_: Int) = dbQuery {
        KardOrders
            .deleteWhere { (kardId eq kardId_).and(columnId eq columnId_) }
    }

    suspend fun recalculateOrders(columnId: Int) = dbQuery {
        val orders = getColumnKards(columnId_ = columnId)
            .sortedBy { it.order }
            .map {
                it.kardId
            }
        updateKardOrders(
            columnId_ = columnId,
            orders = orders.mapIndexed { index, it ->
                NewKardOrder(
                    kardId = it,
                    order = index
                )
            }
        )
    }

    data class KardOrderEntity(
        val id: Int,
        val kardId: Int,
        val columnId: Int,
        val order: Int,
    )

    data class NewKardOrder(
        val kardId: Int,
        val order: Int,
    )
}