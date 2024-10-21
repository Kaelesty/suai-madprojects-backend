package di

import app.Config
import data.schemas.ChatService
import data.schemas.MessageService
import data.schemas.UnreadMessageService
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module

val dataModule = module {

    single<UnreadMessageService> {
        UnreadMessageService(
            database = get()
        )
    }

    single<ChatService> {
        ChatService(
            database = get()
        )
    }

    single<MessageService> {
        MessageService(
            database = get()
        )
    }

    single<Database> {
        with(Config.DatabaseConfig) {
            Database.connect(
                url = url,
                user = user,
                driver = driver,
                password = password
            )
        }
    }
}