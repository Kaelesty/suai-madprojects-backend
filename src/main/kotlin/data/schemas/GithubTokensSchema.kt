package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import shared_domain.entities.GithubUserMeta

class GithubService(
    database: Database
) {

    object GithubTokens: Table() {
        val id = integer("id").autoIncrement()
        val userId = integer("user_id")
            .references(UserService.Users.id)
        val refreshToken = varchar("refresh", length = 512)
        val accessToken = varchar("access", length = 512)

        val refreshExpiresMillis = long("refreshExpiresMillis")
        val accessExpiresMillis = long("accessExpiresMillis")

        val githubAvatar = varchar("avatar", length = 256)
        val githubId = integer("githubId")
        val githubProfileLink = varchar("githubProfile", length = 256)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(GithubTokens)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(
        userId_: String,
        refresh: String,
        access: String,
        refreshExpiresMillis_: Long,
        accessExpiresMillis_: Long,
        githubId_: Int,
        avatar_: String,
        profileLink_: String,
    ) = dbQuery {
        GithubTokens.deleteWhere { userId eq userId_.toInt() }
        GithubTokens.insert {
            it[userId] = userId_.toInt()
            it[refreshToken] = refresh
            it[accessToken] = access
            it[refreshExpiresMillis] = refreshExpiresMillis_
            it[accessExpiresMillis] = accessExpiresMillis_
            it[githubId] = githubId_
            it[githubAvatar] = avatar_
            it[githubProfileLink] = profileLink_
        }
    }

    suspend fun updateTokens(userId_: String, access: String, refresh: String) = dbQuery {
        GithubTokens.update(
            where = { GithubTokens.userId eq userId_.toInt() }
        ) {
            it[accessToken] = access
            it[refreshToken] = refresh
        }
    }

    suspend fun getAccessToken(userId_: String) = dbQuery {
        GithubTokens.selectAll()
            .where(GithubTokens.userId eq userId_.toInt())
            .map { it[GithubTokens.accessToken] to it[GithubTokens.accessExpiresMillis] }
            .firstOrNull()
    }

    suspend fun getRefreshToken(userId_: String) = dbQuery {
        GithubTokens.selectAll()
            .where(GithubTokens.userId eq userId_.toInt())
            .map { it[GithubTokens.refreshToken] to it[GithubTokens.accessExpiresMillis] }
            .firstOrNull()
    }

    suspend fun getUserMeta(githubUserId_: Int) = dbQuery {
        GithubTokens.selectAll()
            .where(GithubTokens.githubId eq githubUserId_)
            .map {
                GithubUserMeta(
                    githubAvatar = it[GithubTokens.githubAvatar],
                    githubId = it[GithubTokens.githubId],
                    profileLink = it[GithubTokens.githubProfileLink],
                    id = it[GithubTokens.userId].toString()
                )
            }
            .firstOrNull()
    }

    suspend fun getUserMeta(userId_: String) = dbQuery {
        GithubTokens.selectAll()
            .where(GithubTokens.userId eq userId_.toInt())
            .map {
                GithubUserMeta(
                    githubAvatar = it[GithubTokens.githubAvatar],
                    githubId = it[GithubTokens.githubId],
                    profileLink = it[GithubTokens.githubProfileLink],
                    id = it[GithubTokens.userId].toString()
                )
            }
            .firstOrNull()
    }

    suspend fun getUserId(githubId_: Int) = dbQuery {
        GithubTokens.selectAll()
            .where(GithubTokens.githubId eq githubId_)
            .map {
                it[GithubTokens.userId]
            }
            .firstOrNull()
    }
}