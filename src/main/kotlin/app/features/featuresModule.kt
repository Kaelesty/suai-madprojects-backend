package app.features

import app.features.auth.AuthFeature
import app.features.auth.AuthFeatureImpl
import app.features.curatorship.CuratorshipFeature
import app.features.curatorship.CuratorshipFeatureImpl
import app.features.github.GithubFeature
import app.features.github.GithubFeatureImpl
import app.features.profile.ProfileFeature
import app.features.profile.ProfileFeatureImpl
import app.features.project.ProjectsFeature
import app.features.project.ProjectsFeatureImpl
import app.features.projectgroups.ProjectGroupsFeature
import app.features.projectgroups.ProjectGroupsFeatureImpl
import data.repos.CuratorshipRepoImpl
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
            jwt = get(),
        )
    }

    single<ProfileFeature> {
        ProfileFeatureImpl(
            profileRepo = get(),
            githubTokensRepo = get(),
            projectsRepo = get(),
            projectsGroupRepo = get(),
        )
    }

    single<ProjectsFeature> {
        ProjectsFeatureImpl(
            projectRepo = get(),
            projectMembershipService = get(),
            repositoriesRepo = get(),
        )
    }

    single<ProjectGroupsFeature> {
        ProjectGroupsFeatureImpl(
            profileRepo = get(),
            projectsGroupRepo = get(),
        )
    }

    single<CuratorshipFeature> {
        CuratorshipFeatureImpl(
            projectMembershipRepo = get(),
            profileRepo = get(),
            curatorshipRepo = get(),
        )
    }
}