package di

import data.repos.AuthRepoImpl
import data.repos.ChatsRepoImpl
import data.repos.GithubTokensRepoImpl
import data.repos.KanbanRepositoryImpl
import data.repos.MessagesRepoImpl
import data.repos.RepositoriesRepoImpl
import data.repos.UnreadMessagesRepoImpl
import domain.GithubTokensRepo
import domain.IntegrationService
import domain.KanbanRepository
import domain.RepositoriesRepo
import domain.UnreadMessagesRepository
import domain.auth.AuthRepo
import entities.User
import entities.UserType
import shared_domain.repos.ChatsRepository
import shared_domain.repos.MessagesRepository
import org.koin.dsl.module

val domainModule = module {

    single<AuthRepo> {
        AuthRepoImpl(
            userService = get(),
            commonUsersDataService = get(),
            curatorsDataService = get()
        )
    }

    single<UnreadMessagesRepository> {
        UnreadMessagesRepoImpl(
            unreadMessageService = get()
        )
    }

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

    single<RepositoriesRepo> {
        RepositoriesRepoImpl()
    }

    single<KanbanRepository> {
        KanbanRepositoryImpl(
            kardService = get(),
            kardOrdersService = get(),
            columnsService = get()
        )
    }

    single<GithubTokensRepo> {
        GithubTokensRepoImpl(
            githubService = get()
        )
    }

    single<IntegrationService> {
        // TODO
        object : IntegrationService {

            override fun getChatMembersIds(chatId: Int): List<String> {
                return listOf("1", "2", "3")
            }

            override fun getUserFromJWT(jwt: String): User? {
                return User(
                    id = jwt,
                    type = UserType.COMMON
                )
            }

            override fun getProjectUsers(projectId: Int): List<User> {
                return listOf(
                    User("1", UserType.COMMON),
                    User("2", UserType.COMMON),
                    User("3", UserType.CURATOR)
                )
            }
        }
    }
}