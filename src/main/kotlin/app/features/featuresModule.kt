package app.features

import app.REQUIRE_USER_IN_PROJECT_QUALIFIER
import app.features.auth.AuthFeature
import app.features.auth.AuthFeatureImpl
import app.features.github.GithubFeature
import app.features.github.GithubFeatureImpl
import app.requireUserInProject
import io.ktor.server.routing.RoutingContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featuresModule = module {

    single<KardsFeature> {
        KardsFeatureImpl(
            kanbanRepository = get(),
            projectRepo = get(),
        )
    }

    single<SprintsFeature> {
        SprintsFeatureImpl(
            projectRepo = get(),
            sprintsRepo = get()
        )
    }

    single<GithubFeature> {
        GithubFeatureImpl(
            githubTokensRepo = get(),
            repositoriesRepo = get(),
            httpClient = get(),
            jwt = get()
        )
    }

    single<WsFeature> {
        WsFeatureImpl(
            integrationRepo = get(),
            kanbanRepository = get(),
            messagesRepo = get(),
            unreadMessagesRepo = get(),
            chatsRepo = get(),
        )
    }

    single<SwaggerFeature>  {
        SwaggerFeatureImpl()
    }

    single<AuthFeature> {
        AuthFeatureImpl(
            authRepo = get(),
            jwt = get()
        )
    }

    single<ProfileFeature> {
        ProfileFeatureImpl(
            profileRepo = get(),
            githubTokensRepo = get(),
            projectsRepo = get(),
        )
    }

    single<ProjectsFeature> {
        ProjectsFeatureImpl(
            projectRepo = get()
        )
    }
}