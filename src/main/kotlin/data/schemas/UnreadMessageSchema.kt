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
            .references(MessageService.Messages.id)
        val userId = varchar("user_id", length = 128)
        val chatId = integer("chat_id")
            .references(ChatService.Chats.id)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(UnreadMessage)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(userId_: String, messageId_: Int, chatId_: Int) = dbQuery {
        UnreadMessage.insert {
            it[userId] = userId_
            it[messageId] = messageId_
            it[chatId] = chatId_
        }[UnreadMessage.id]
    }

    suspend fun getUserUnreadMessages(userId_: String, chatId_: Int) = dbQuery {
        UnreadMessage.selectAll()
            .where { (UnreadMessage.userId eq userId_) .and (UnreadMessage.chatId eq chatId_) }
            .map {
                UnreadMessage(
                    id = it[UnreadMessage.id],
                    userId = it[UnreadMessage.userId],
                    messageId = it[UnreadMessage.messageId]
                )
            }
    }

    suspend fun deleteUnreadMessage(userId_: String, messageId_: Int) = dbQuery {
        UnreadMessage
            .deleteWhere { (userId eq userId_) .and (messageId eq messageId_) }
    }
}