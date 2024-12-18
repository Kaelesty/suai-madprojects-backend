package app.features

import app.features.activity.ActivityFeature
import app.features.activity.ActivityFeatureImpl
import app.features.analytics.AnalyticsFeature
import app.features.analytics.AnalyticsFeatureImpl
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
import app.features.sprints.SprintsFeature
import app.features.sprints.SprintsFeatureImpl
import org.koin.dsl.module

val featuresModule = module {

    single<KardsFeature> {
        KardsFeatureImpl(
            kanbanRepository = get(),
            projectRepo = get(),
        )
    }

    single<AnalyticsFeature> {
        AnalyticsFeatureImpl(
            projectRepo = get(),
            projectGroupsRepo = get()
        )
    }

    single<SprintsFeature> {
        SprintsFeatureImpl(
            projectRepo = get(),
            sprintsRepo = get(),
            activityRepo = get(),
        )
    }

    single<ActivityFeature> {
        ActivityFeatureImpl(
            activityRepo = get(),
            profileRepo = get(),
            projectRepo = get(),
        )
    }

    single<GithubFeature> {
        GithubFeatureImpl(
            githubTokensRepo = get(),
            repositoriesRepo = get(),
            httpClient = get(),
            jwt = get(),
            profileRepo = get(),
        )
    }

    single<InvitesFeature> {
        InvitesFeatureImpl(
            invitesRepo = get(),
            projectRepo = get(),
            activityRepo = get(),
            profileRepo = get()
        )
    }

    single<WsFeature> {
        WsFeatureImpl(
            kanbanRepository = get(),
            messagesRepo = get(),
            unreadMessagesRepo = get(),
            chatsRepo = get(),
            jwt = get(),
            profileRepo = get(),
            projectCuratorshipService = get(),
            projectMembershipService = get(),
            githubTokensRepo = get(),
            activityRepo = get()
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
            repositoriesRepo = get(),
            activityRepo = get(),
            profileRepo = get()
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
            projectRepo = get(),
            profileRepo = get(),
            curatorshipRepo = get(),
        )
    }
}