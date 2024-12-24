package data.schemas

import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class KardService(
    database: Database
) {

    object Kards : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val authorId = integer("authorId")
            .references(UserService.Users.id)
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

    suspend fun create(name_: String, desc_: String, authorId_: String) = dbQuery {
        Kards.insert {
            it[name] = name_
            it[desc] = desc_
            it[createTimeMillis] = System.currentTimeMillis()
            it[updateTimeMillis] = System.currentTimeMillis()
            it[authorId] = authorId_.toInt()
        }[Kards.id]
    }

    suspend fun update(id_: Int, name_: String?, desc_: String?) = dbQuery {
        Kards.update(
            where = { Kards.id eq id_ }
        ) {
            name_?.let { name_ -> it[name] = name_ }
            desc_?.let { desc_ -> it[desc] = desc_ }
            it[updateTimeMillis] = System.currentTimeMillis()
        }
    }

    suspend fun updateKardTime(kardId: Int) = dbQuery {
        Kards.update(
            where = { Kards.id eq kardId }
        ) {
            it[updateTimeMillis] = System.currentTimeMillis()
        }
    }

    suspend fun deleteKard(id_: Int) = dbQuery {
        Kards.deleteWhere { id eq id_}
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
                    authorName = it[Kards.authorId].toString() // TODO
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
        val authorName: String,
    )
}