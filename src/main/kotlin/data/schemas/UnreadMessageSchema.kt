package data.schemas

import entities.Chat
import entities.ChatType
import entities.UnreadMessage
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class UnreadMessageService(
    database: Database
) {

    object UnreadMessage : Table() {
        val id = integer("id").autoIncrement()
        val messageId = integer("message_id")
        val userId = integer("user_id")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns()
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(userId_: Int, messageId_: Int) = dbQuery {
        UnreadMessage.insert {
            it[userId] = userId_
            it[messageId] = messageId_
        }[UnreadMessage.id]
    }

    suspend fun getUserUnreadMessages(userId_: Int) = dbQuery {
        UnreadMessage.selectAll()
            .where { UnreadMessage.userId eq userId_ }
            .map {
                entities.UnreadMessage(
                    id = it[UnreadMessage.id],
                    userId = it[UnreadMessage.userId],
                    messageId = it[UnreadMessage.messageId]
                )
            }
    }

    suspend fun deleteUnreadMessage(unreadMessageId: Int) = dbQuery {
        UnreadMessage
            .deleteWhere(limit = 1) { id eq unreadMessageId }
    }
}