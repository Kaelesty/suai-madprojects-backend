package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class CommonUsersDataService(
    database: Database
) {

    object CommonUsersData : Table() {
        val id = integer("id").autoIncrement()
        val userId = integer("user_id")
            .references(UserService.Users.id)
        val group = varchar("group", length = 24)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(CommonUsersData)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(userId_: Int, group_: String) = dbQuery {
        CommonUsersData.insert {
            it[userId] = userId_
            it[group] = group_
        }
    }

    suspend fun getByUser(userId_: Int) = dbQuery {
        CommonUsersData.selectAll()
            .where { CommonUsersData.userId eq userId_ }
            .map {
                it[CommonUsersData.group]
            }
            .firstOrNull()
    }
}