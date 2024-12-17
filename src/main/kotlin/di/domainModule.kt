package di

import data.repos.ActivityRepoImpl
import data.repos.AuthRepoImpl
import data.repos.ChatsRepoImpl
import data.repos.CuratorshipRepoImpl
import data.repos.GithubTokensRepoImpl
import data.repos.InvitesRepoImpl
import data.repos.KanbanRepositoryImpl
import data.repos.MessagesRepoImpl
import data.repos.ProfileRepoImpl
import data.repos.ProjectGroupsRepoImpl
import data.repos.ProjectRepoImpl
import data.repos.RepositoriesRepoImpl
import data.repos.SprintsRepoImpl
import data.repos.UnreadMessagesRepoImpl
import domain.CuratorshipRepo
import domain.GithubTokensRepo
import domain.IntegrationService
import domain.InvitesRepo
import domain.KanbanRepository
import domain.RepositoriesRepo
import domain.UnreadMessagesRepository
import domain.activity.ActivityRepo
import domain.auth.AuthRepo
import domain.profile.ProfileRepo
import domain.project.ProjectRepo
import domain.projectgroups.ProjectsGroupRepo
import domain.sprints.SprintsRepo
import entities.User
import entities.UserType
import shared_domain.repos.ChatsRepository
import shared_domain.repos.MessagesRepository
import org.koin.dsl.module

val domainModule = module {

    single<ProjectRepo> {
        ProjectRepoImpl(
            userService = get(),
            projectService = get(),
            projectMembershipService = get(),
            projectCuratorshipService = get(),
            projectReposService = get(),
            chatsService = get(),
            columnsService = get(),
        )
    }

    single<ProjectsGroupRepo> {
        ProjectGroupsRepoImpl(
            projectMembershipService = get(),
            projectService = get(),
            projectCuratorshipService = get(),
            projectGroupsService = get(),
            userService = get(),
            commonUsersDataService = get()
        )
    }

    single<CuratorshipRepo> {
        CuratorshipRepoImpl(
            curatorshipService = get(),
            unapprovedProjectService = get(),
            projectService = get(),
            projectMembershipService = get(),
            userService = get(),
            commonUsersDataService = get()
        )
    }

    single<ProfileRepo> {
        ProfileRepoImpl(
            usersService = get(),
            commonUsersDataService = get(),
            curatorsDataService = get(),
            githubService = get(),
        )
    }

    single<SprintsRepo> {
        SprintsRepoImpl(
            sprintsService = get(),
            kardsInSprintsService = get(),
        )
    }

    single<ActivityRepo> {
        ActivityRepoImpl(
            activityService = get()
        )
    }

    single<InvitesRepo> {
        InvitesRepoImpl(
            invitesService = get(),
            projectMembershipService = get()
        )
    }

    single<AuthRepo> {
        AuthRepoImpl(
            userService = get(),
            commonUsersDataService = get(),
            curatorsDataService = get(),
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
            integrationService = get(),
            chatService = get(),
            projectMembershipService = get(),
            projectCuratorshipService = get(),
        )
    }

    single<RepositoriesRepo> {
        RepositoriesRepoImpl(
            projectsReposService = get()
        )
    }

    single<KanbanRepository> {
        KanbanRepositoryImpl(
            kardService = get(),
            kardOrdersService = get(),
            columnsService = get(),
            kardInSprintService = get(),
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