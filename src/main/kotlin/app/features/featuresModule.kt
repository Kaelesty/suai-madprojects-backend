package app.features

import app.features.auth.AuthFeature
import app.features.auth.AuthFeatureImpl
import org.koin.dsl.module
import kotlin.math.sin

val featuresModule = module {
    single<GithubFeature> {
        GithubFeatureImpl(
            integrationRepo = get(),
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
            githubTokensRepo = get()
        )
    }

    single<ProjectsFeature> {
        ProjectsFeatureImpl(
            projectRepo = get()
        )
    }
}