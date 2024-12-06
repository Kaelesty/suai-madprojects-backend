package data.schemas

import entities.Message
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class GithubService(
    database: Database
) {

    object GithubTokens: Table() {
        val id = integer("id").autoIncrement()
        val userId = integer("user_id")
            //.references(ChatService.Chats.id) TODO reference
        val refreshToken = varchar("refresh", length = 512)
        val accessToken = varchar("access", length = 512)

        val refreshExpiresMillis = long("refreshExpiresMillis")
        val accessExpiresMillis = long("accessExpiresMillis")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(GithubTokens)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(userId_: Int, refresh: String, access: String, refreshExpiresMillis_: Long, accessExpiresMillis_: Long) = dbQuery {
        GithubTokens.insert {
            it[userId] = userId_
            it[refreshToken] = refresh
            it[accessToken] = access
            it[refreshExpiresMillis] = refreshExpiresMillis_
            it[accessExpiresMillis] = accessExpiresMillis_
        }
    }
}