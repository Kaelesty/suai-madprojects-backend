package di

import data.repos.ChatsRepoImpl
import data.repos.MessagesRepoImpl
import shared_domain.repos.ChatsRepository
import shared_domain.repos.MessagesRepository
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