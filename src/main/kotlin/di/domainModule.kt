package di

import data.repos.ChatsRepoImpl
import data.repos.MessagesRepoImpl
import domain.IntegrationService
import entities.User
import entities.UserType
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
            messageService = get(),
            unreadMessageService = get(),
            integrationService = get()
        )
    }

    single<IntegrationService> {
        // TODO
        object : IntegrationService {

            override fun getChatMembersIds(chatId: Int): List<Int> {
                return listOf(1, 2, 3)
            }

            override fun getUserFromJWT(jwt: String): User {
                return User(
                    id = jwt.toInt(),
                    type = UserType.DEFAULT
                )
            }

            override fun getProjectUsers(projectId: Int): List<User> {
                return listOf(
                    User(1, UserType.DEFAULT),
                    User(2, UserType.DEFAULT),
                    User(3, UserType.CURATOR)
                )
            }
        }
    }
}