package data

import app.Config
import data.schemas.ChatService
import data.schemas.ColumnsService
import data.schemas.CommonUsersDataService
import data.schemas.CuratorsDataService
import data.schemas.GithubService
import data.schemas.KardOrdersService
import data.schemas.KardService
import data.schemas.MessageService
import data.schemas.UnreadMessageService
import data.schemas.UserService
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

    single<KardService> {
        KardService(
            database = get()
        )
    }

    single<KardOrdersService> {
        KardOrdersService(
            database = get()
        )
    }

    single<ColumnsService> {
        ColumnsService(
            database = get()
        )
    }

    single<GithubService> {
        GithubService(
            database = get()
        )
    }

    single<UserService> {
        UserService(
            database = get()
        )
    }

    single<CommonUsersDataService> {
        CommonUsersDataService(
            database = get()
        )
    }

    single<CuratorsDataService> {
        CuratorsDataService(
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