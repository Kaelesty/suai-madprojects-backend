package data.schemas

import entities.Message
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class MessageService(
    database: Database
) {

    object Messages: Table() {
        val id = integer("id").autoIncrement()
        val chatId = integer("chat_id")
            .references(ChatService.Chats.id)
        val senderId = integer("sender_id")
        val text = varchar("text", length = 256)
        val createdTimeMillis = long("createdTimeMillis")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Messages)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(chatId_: Int, senderId_: String, text_: String, createTimeMillis_: Long) = dbQuery {
        val newId = Messages.insert {
            it[chatId] = chatId_
            it[senderId] = senderId_.toInt()
            it[text] = text_
            it[createdTimeMillis] = createTimeMillis_
        }[Messages.id]
        return@dbQuery Messages.selectAll()
            .where { Messages.id eq newId }
            .map {
                Message(
                    id = it[Messages.id],
                    text = it[Messages.text],
                    senderId = it[Messages.senderId].toString(),
                    time = it[Messages.createdTimeMillis]
                )
            }
            .first()
    }

    suspend fun getChatMessages(chatId_: Int) = dbQuery {
        Messages.selectAll()
            .where { Messages.chatId eq chatId_ }
            .map {
                Message(
                    id = it[Messages.id],
                    text = it[Messages.text],
                    senderId = it[Messages.senderId].toString(),
                    time = it[Messages.createdTimeMillis]
                )
            }
    }
}