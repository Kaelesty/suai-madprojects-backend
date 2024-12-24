package data.schemas

import entities.Chat
import entities.ChatType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ChatService(
    database: Database
) {

    object Chats: Table() {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)
        val title = varchar("title", length = 25)
        val chatType = enumerationByName<ChatType>("chat_type", 15)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Chats)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(projectId_: Int, title_: String, chatType_: ChatType) = dbQuery {
        val newId = Chats.insert {
            it[projectId] = projectId_
            it[title] = title_
            it[chatType] = chatType_
        }[Chats.id]
        return@dbQuery Chats.selectAll()
            .where { Chats.id eq newId }
            .map {
                Chat(
                    id = it[Chats.id],
                    chatType = it[Chats.chatType],
                    title = it[Chats.title],
                    lastMessage = null,
                    unreadMessagesCount = 0
                )
            }
            .first()

    }

    suspend fun getProjectChats(projectId_: Int, chatType_: ChatType) = dbQuery {
        Chats.selectAll()
            .where { (Chats.projectId eq projectId_) and (Chats.chatType eq chatType_)}
            .map {
                Chat(
                    id = it[Chats.id],
                    chatType = it[Chats.chatType],
                    title = it[Chats.title],
                    lastMessage = null,
                    unreadMessagesCount = 0
                )
            }
    }

    suspend fun getById(chatId_: Int) = dbQuery {
        Chats.selectAll()
            .where { Chats.id eq chatId_ }
            .map {
                Chat(
                    id = it[Chats.id],
                    chatType = it[Chats.chatType],
                    title = it[Chats.title],
                    lastMessage = null,
                    unreadMessagesCount = 0
                )
            }
            .first()
    }
}