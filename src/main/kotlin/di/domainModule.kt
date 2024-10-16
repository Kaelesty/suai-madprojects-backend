package di

import data.repos.ChatsRepoImpl
import data.repos.MessagesRepoImpl
import domain.repos.ChatsRepository
import domain.repos.MessagesRepository
import org.koin.dsl.module

val domainModule = module {

    single<ChatsRepository> {
        ChatsRepoImpl(
            chatService = get()
        )
    }

    single<MessagesRepository> {
        MessagesRepoImpl(
            messageService = get()
        )
    }
}