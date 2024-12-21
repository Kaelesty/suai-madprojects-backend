package data

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import data.schemas.ActivityService
import data.schemas.ChatService
import data.schemas.ColumnsService
import data.schemas.CommonUsersDataService
import data.schemas.CuratorsDataService
import data.schemas.GithubService
import data.schemas.InvitesService
import data.schemas.KardInSprintService
import data.schemas.KardOrdersService
import data.schemas.KardService
import data.schemas.MessageService
import data.schemas.ProjectCuratorshipService
import data.schemas.ProjectGroupService
import data.schemas.ProjectMembershipService
import data.schemas.ProjectReposService
import data.schemas.ProjectService
import data.schemas.SprintsService
import data.schemas.UnapprovedProjectService
import data.schemas.UnreadMessageService
import data.schemas.UserService
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module

val dataModule = module {

    single<SprintsService> {
        SprintsService(
            database = get()
        )
    }

    single<ActivityService> {
        ActivityService(
            database = get()
        )
    }

    single<KardInSprintService> {
        KardInSprintService(
            database = get()
        )
    }

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

    single<InvitesService> {
        InvitesService(
            database = get()
        )
    }

    single<MessageService> {
        MessageService(
            database = get()
        )
    }

    single<ProjectGroupService> {
        ProjectGroupService(
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

    single<ProjectService> {
        ProjectService(
            database = get()
        )
    }

    single<ProjectReposService> {
        ProjectReposService(
            database = get()
        )
    }

    single<ProjectMembershipService> {
        ProjectMembershipService(
            database = get()
        )
    }

    single<ProjectCuratorshipService> {
        ProjectCuratorshipService(
            database = get()
        )
    }

    single<UnapprovedProjectService> {
        UnapprovedProjectService(
            database = get()
        )
    }

    single<Database> {
        with(get<app.config.Config>()) {
            Database.connect(
                url = db.url,
                user = db.user,
                driver = db.driver,
                password = db.password
            )
        }
    }

    single<JWTVerifier> {
        with(get<app.config.Config>()) {
            JWT.require(Algorithm.HMAC256(auth.jwtSecret))
                .withAudience(ssl.domain)
                .withIssuer(ssl.domain)
                .build()
        }
    }
}