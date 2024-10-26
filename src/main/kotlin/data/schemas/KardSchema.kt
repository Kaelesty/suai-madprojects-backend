package data.schemas

import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class KardService(
    database: Database
) {

    object Kards : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 25)
        val desc = varchar("desc", length = 1024)
        val createTimeMillis = long("create_time_millis")
        val updateTimeMillis = long("update_time_millis")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Kards)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(name_: String, desc_: String) = dbQuery {
        Kards.insert {
            it[name] = name_
            it[desc] = desc_
            it[createTimeMillis] = System.currentTimeMillis()
            it[updateTimeMillis] = System.currentTimeMillis()
        }[Kards.id]
    }

    suspend fun updateKardTime(kardId: Int) = dbQuery {
        Kards.update(
            where = { Kards.id eq kardId }
        ) {
            it[updateTimeMillis] = System.currentTimeMillis()
        }
    }

    suspend fun getById(kardId: Int) = dbQuery {
        Kards.selectAll()
            .where { Kards.id eq kardId }
            .mapLazy {
                KardEntity(
                    id = it[Kards.id],
                    name = it[Kards.name],
                    desc = it[Kards.desc],
                    createTimeMillis = it[Kards.createTimeMillis],
                    updateTimeMillis = it[Kards.updateTimeMillis],
                )
            }
            .first()
    }

    data class KardEntity(
        val id: Int,
        val name: String,
        val desc: String,
        val createTimeMillis: Long,
        val updateTimeMillis: Long,
    )
}