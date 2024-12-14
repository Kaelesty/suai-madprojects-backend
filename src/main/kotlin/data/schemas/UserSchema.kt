package data.schemas

import domain.auth.User
import domain.auth.UserType
import domain.project.AvailableCurator
import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class UserService(
    database: Database
) {

    object Users: Table() {
        val id = integer("id").autoIncrement()
        val username = varchar("username", length = 24)
        val password = varchar("password", length = 256)
        val lastName = varchar("lastname", length = 24)
        val firstName = varchar("firstname", length = 24)
        val secondName = varchar("secondname", length = 24)
        val email = varchar("title", length = 64)
        val userType = enumerationByName<UserType>("userType", 24)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Users)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(
        username_: String,
        password_: String,
        lastName_: String,
        firstName_: String,
        secondName_: String,
        email_: String,
        userType_: UserType
    ) = dbQuery {
        val newId = Users.insert {
            it[username] = username_
            it[password] = password_
            it[lastName] = lastName_
            it[firstName] = firstName_
            it[secondName] = secondName_
            it[email] = email_
            it[userType] = userType_
        }[Users.id]

        return@dbQuery newId.toString()
    }

    suspend fun isEmailUnique(email: String): Boolean = dbQuery {
        Users.selectAll()
            .where {Users.email eq email}
            .count() == 0L
    }

    suspend fun isUsernameUnique(username: String): Boolean = dbQuery {
        Users.selectAll()
            .where {Users.username eq username}
            .count() == 0L
    }

    suspend fun getById(userId_: Int) = dbQuery {
        Users.selectAll()
            .where { Users.id eq userId_ }
            .map {
                User(
                    id = it[Users.id].toString(),
                    username = it[Users.username],
                    password = it[Users.password],
                    lastName = it[Users.lastName],
                    firstName = it[Users.firstName],
                    secondName = it[Users.secondName],
                    email = it[Users.email],
                    userType = it[Users.userType]
                )
            }
            .firstOrNull()
    }

    suspend fun getByEmail(email_: String) = dbQuery {
        Users.selectAll()
            .where { Users.email eq email_ }
            .map {
                User(
                    id = it[Users.id].toString(),
                    username = it[Users.username],
                    password = it[Users.password],
                    lastName = it[Users.lastName],
                    firstName = it[Users.firstName],
                    secondName = it[Users.secondName],
                    email = it[Users.email],
                    userType = it[Users.userType]
                )
            }
            .firstOrNull()
    }

    suspend fun getCurators() = dbQuery {
        Users.selectAll()
            .where { Users.userType eq UserType.Curator }
            .map {
                AvailableCurator(
                    firstName = it[Users.firstName],
                    secondName = it[Users.secondName],
                    lastName = it[Users.lastName],
                    id = it[Users.id].toString(),
                    username = it[Users.username]
                )
            }
    }

    suspend fun update(
        userId_: String,
        firstName_: String?,
        secondName_: String?,
        lastName_: String?,
    ) = dbQuery {
        Users.update(
            where = { Users.id eq userId_.toInt() }
        ) {
            firstName_?.let { firstName_ ->
                it[firstName] = firstName_
            }
            secondName_?.let { secondName_ ->
                it[secondName] = secondName_
            }
            lastName_?.let { lastName_ ->
                it[lastName] = lastName_
            }
        }
    }
}