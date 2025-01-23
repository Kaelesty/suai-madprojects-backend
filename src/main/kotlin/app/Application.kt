package app

import app.features.InvitesFeature
import app.features.KardsFeature
import app.features.MarksFeature
import app.features.WsFeature
import app.features.activity.ActivityFeature
import app.features.analytics.AnalyticsFeature
import app.features.auth.AuthFeature
import app.features.curatorship.CuratorshipFeature
import app.features.github.GithubFeature
import app.features.profile.ProfileFeature
import app.features.project.ProjectsFeature
import app.features.projectgroups.ProjectGroupsFeature
import app.features.sprints.SprintsFeature
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.io.File
import java.security.KeyStore

class Application : KoinComponent {

    private val githubFeature by inject<GithubFeature>()
    private val wsFeature by inject<WsFeature>()
    private val authFeature by inject<AuthFeature>()
    private val profileFeature by inject<ProfileFeature>()
    private val projectsFeature by inject<ProjectsFeature>()
    private val sprintsFeature by inject<SprintsFeature>()
    private val kardsFeature by inject<KardsFeature>()
    private val projectGroupsFeature by inject<ProjectGroupsFeature>()
    private val curatorshipFeature by inject<CuratorshipFeature>()
    private val invitesFeature by inject<InvitesFeature>()
    private val activityFeature by inject<ActivityFeature>()
    private val analyticsFeature by inject<AnalyticsFeature>()
    private val marksFeature by inject<MarksFeature>()

    private val config = get<app.config.Config>()

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    init {
        setup()
    }

    fun run() {
        server.start(wait = true)
    }

    private fun setup() {
        server = embeddedServer(
            Netty,
            environment = applicationEnvironment {
            },
            {
                val keyStoreFile = File("src/main/resources/keystore.p12")

                connector {
                    port = 8079
                }

                sslConnector(
                    keyStore = KeyStore.getInstance("PKCS12").apply {
                        load(
                            File("src/main/resources/keystore.p12").inputStream(),
                            config.ssl.certificatePassword.toString().toCharArray()
                        )
                    },
                    keyAlias = config.ssl.certificateAlias,
                    keyStorePassword = { config.ssl.certificatePassword.toString().toCharArray() },
                    privateKeyPassword = { config.ssl.certificatePassword.toString().toCharArray() }
                ) {
                    port = 8080
                    keyStorePath = keyStoreFile
                }
            }
        ) {
            install(WebSockets)

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }

            authFeature.install_(this)

            install(CORS) {
                allowHost(config.ssl.domain, schemes = listOf("https"))
                allowHost("localhost:3000")
                allowHeader("code")
                allowHeader("state")
                allowHeader("repolink")
                allowHeader("Authorization")
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Delete)
                allowMethod(HttpMethod.Patch)
                allowHeader(HttpHeaders.AccessControlAllowHeaders)
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.AccessControlAllowOrigin)
                allowCredentials = true
            }

            routing {

                post("/auth/login") {
                    authFeature.login(this)
                }

                post("/auth/register") {
                    authFeature.register(this)
                }

                authenticate("auth-jwt") {

                    get("/hello") {
                        val principal = call.principal<JWTPrincipal>()
                        val username = principal!!.payload.getClaim("username").asString()
                        val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                        call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
                    }

                    post("/auth/refresh") {
                        authFeature.refresh(this)
                    }

                    get("/analytics/getGroups") {
                        analyticsFeature.getGroups(this)
                    }

                    get("/analytics/projectStatuses") {
                        analyticsFeature.getProjectStatusesInProjectGroup(this)
                    }

                    get("/analytics/projectStatusesByProjectId") {
                        analyticsFeature.getProjectStatusesInProjectGroupByProject(this)
                    }

                    get("/analytics/userCommits") {
                        analyticsFeature.getCommitsByUsersInProject(this)
                    }

                    get("/analytics/projectGroupCommits") {
                        analyticsFeature.getCommitsByProjectInProjectGroup(this)
                    }

                    get("/analytics/groupMarks") {
                        analyticsFeature.getGroupMembersWithMarks(this)
                    }

                    get("/analytics/projectGroupMarks") {
                        analyticsFeature.getProjectMarksInProjectGroup(this)
                    }

                    get("/sharedProfile") {
                        profileFeature.getSharedProfile(this)
                    }

                    get("/commonProfile") {
                        profileFeature.getCommonProfile(this)
                    }

                    get("/curatorProfile") {
                        profileFeature.getCuratorProfile(this)
                    }

                    post("/commonProfile/update") {
                        profileFeature.updateCommonProfile(this)
                    }

                    post("/curatorProfile/update") {
                        profileFeature.updateCuratorProfile(this)
                    }

                    get("/github/getUserMeta") {
                        githubFeature.getUserMeta(this)
                    }

                    get("/github/getRepoBranchContent") {
                        githubFeature.getRepoBranchContent(this)
                    }

                    get("/github/getProjectRepoBranches") {
                        githubFeature.getProjectRepoBranches(this)
                    }

                    get("/github/verifyRepoLink") {
                        githubFeature.verifyRepoLink(this)
                    }

                    get("/project/curators") {
                        projectsFeature.getCurators(this)
                    }

                    post("/project/create") {
                        projectsFeature.createProject(this)
                    }

                    get("/project/kards") {
                        kardsFeature.getProjectKards(this)
                    }

                    get("/project/get") {
                        projectsFeature.getProject(this)
                    }

                    post("/project/repo/remove") {
                        projectsFeature.removeRepository(this)
                    }

                    post("/project/repo/add") {
                        projectsFeature.addRepository(this)
                    }

                    post("/project/update") {
                        projectsFeature.updateProjectMeta(this)
                    }

                    post("/project/member/remove") {
                        projectsFeature.removeMember(this)
                    }

                    post("/project/delete") {
                        projectsFeature.deleteProject(this)
                    }

                    post("/project/mark/set") {
                        marksFeature.markProject(this)
                    }

                    get("/project/mark/get") {
                        marksFeature.getProjectMark(this)
                    }

                    post("/projectgroup/create") {
                        projectGroupsFeature.createProjectsGroup(this)
                    }

                    get("/projectgroup/getCuratorGroups") {
                        projectGroupsFeature.getCuratorProjectGroups(this)
                    }

                    get("/projectgroup/getGroupProjects") {
                        projectGroupsFeature.getGroupProjects(this)
                    }

                    get("/curatorship/getProjects") {
                        projectGroupsFeature.getCuratorProjects(this)
                    }

                    post("/curatorship/retrySubmission") {
                        curatorshipFeature.retrySubmission(this)
                    }

                    post("/curatorship/approve") {
                        curatorshipFeature.approveProject(this)
                    }

                    post("/curatorship/disapprove") {
                        curatorshipFeature.disapproveProject(this)
                    }

                    get("/curatorship/getPendingProjects") {
                        curatorshipFeature.getPendingProjects(this)
                    }

                    get("/curatorship/getUnmarkedProjects") {
                        curatorshipFeature.getUnmarkedProjects(this)
                    }

                    post("/sprint/create") {
                        sprintsFeature.createSprint(this)
                    }

                    get("/sprint/getListByProject") {
                        sprintsFeature.getProjectSprints(this)
                    }

                    post("/sprint/finish") {
                        sprintsFeature.finishSprint(this)
                    }

                    get("/sprint/get") {
                        sprintsFeature.getSprint(this)
                    }

                    post("/sprint/update") {
                        sprintsFeature.updateSprint(this)
                    }

                    get("/sprint/kanban/get") {
                        kardsFeature.getSprintKanban(this)
                    }

                    get("/invites/get") {
                        invitesFeature.getProjectInvite(this)
                    }

                    post("/invites/use") {
                        invitesFeature.useInvite(this)
                    }

                    post("/invites/refresh") {
                        invitesFeature.refreshProjectInvite(this)
                    }

                    get("project/activity/get") {
                        activityFeature.getActivity(this)
                    }

                }

                //swaggerFeature.install(this)

                get("/github/githubCallbackUrl") {
                    githubFeature.proceedGithubApiCallback(this)
                }

                webSocket("/project") {
                    wsFeature.install(this)
                }
            }
        }
    }
}