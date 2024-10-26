package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class KardOrdersService(
    database: Database
) {

    object KardOrders: Table() {
        val id = integer("id").autoIncrement()
        val kardId = integer("kard_id")
            .references(KardService.Kards.id)
        val rowId = integer("row_id")
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
                .where { KardOrders.rowId eq columnId_ }
                .map {
                    it[KardOrders.order]
                }
                .max()
        }

        KardOrders.insert {
            it[kardId] = kardId_
            it[rowId] = columnId_
            it[order] = lastOrder + 1
        }
    }

    suspend fun getRowKards(rowId_: Int) = dbQuery {
        KardOrders.selectAll()
            .where { KardOrders.rowId eq rowId_ }
            .map {
                KardOrderEntity(
                    id = it[KardOrders.id],
                    kardId = it[KardOrders.kardId],
                    rowId = it[KardOrders.rowId],
                    order = it[KardOrders.order]
                )
            }
    }

    suspend fun updateKardOrders(rowId_: Int, orders: List<NewKardOrder>) = dbQuery {
        val kards = getRowKards(rowId_)
        if (kards.size != orders.size) {
            throw IllegalStateException("Count on new orders a'nt match actual count of kards")
        }
        orders.forEach { new ->
            KardOrders.update(
                where = { KardOrders.id eq new.kardId }
            ) {
                it[order] = new.order
            }
        }
    }

    data class KardOrderEntity(
        val id: Int,
        val kardId: Int,
        val rowId: Int,
        val order: Int,
    )

    data class NewKardOrder(
        val kardId: Int,
        val order: Int,
    )
}