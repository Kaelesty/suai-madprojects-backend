package data.schemas

import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class CuratorsDataService(
    database: Database
) {

    object CuratorsData : Table() {
        val id = integer("id").autoIncrement()
        val userId = integer("user_id")
            .references(UserService.Users.id)
        val grade = varchar("grade", length = 64)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(CuratorsData)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(userId_: Int, grade_: String) = dbQuery {
        CuratorsData.insert {
            it[userId] = userId_
            it[grade] = grade_
        }
    }

    suspend fun getByUser(userId_: Int) = dbQuery {
        CuratorsData.selectAll()
            .where { CuratorsData.userId eq userId_ }
            .map {
                it[CuratorsData.grade]
            }
            .firstOrNull()
    }
}