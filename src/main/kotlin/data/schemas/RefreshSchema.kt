package data.schemas

import domain.activity.ActivityType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq


class RefreshService(
    database: Database
) {

    object Refresh : Table() {
        val id = integer("id").autoIncrement()
        val userId = integer("userId")
            .references(UserService.Users.id)
        val request = varchar("request", 128)
        val expireTimeMillis = long("expireTimeMillis")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Refresh)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(
        userId_: Int,
        request_: String,
        expiresAt_: Long
    ) = dbQuery {
        Refresh.insert {
            it[userId] = userId_
            it[request] = request_
            it[expireTimeMillis] = expiresAt_
        }
        Unit
    }

    suspend fun check(request_: String) = dbQuery {
        var id = -1
        Refresh.selectAll()
            .where { Refresh.request eq request_ }
            .map {
                id = it[Refresh.id]
                it[Refresh.userId]
            }
            .firstOrNull()
            .also {
                if (id != -1) {
                    Refresh.deleteWhere {
                        Refresh.id eq id
                    }
                }
            }
    }

    suspend fun delete(request_: String) = dbQuery {
        Refresh.deleteWhere {
            request eq request_
        }
    }

    suspend fun deleteExpired() = dbQuery {
        val currentTimeMillis = System.currentTimeMillis()
        Refresh.deleteWhere {
            expireTimeMillis greaterEq currentTimeMillis
        }
    }

    suspend fun checkUnique(request_: String) = dbQuery {
        Refresh.selectAll()
            .where { Refresh.request eq request_ }
            .count() > 0
    }
}