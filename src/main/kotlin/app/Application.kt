package app

import app.features.InvitesFeature
import app.features.KardsFeature
import app.features.github.GithubFeature
import app.features.profile.ProfileFeature
import app.features.project.ProjectsFeature
import app.features.sprints.SprintsFeature
import app.features.WsFeature
import app.features.auth.AuthFeature
import app.features.curatorship.CuratorshipFeature
import app.features.projectgroups.ProjectGroupsFeature
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.security.KeyStore
import io.ktor.server.plugins.contentnegotiation.*

class Application : KoinComponent {

    private val githubFeature: GithubFeature by inject()
    private val wsFeature: WsFeature by inject()
    private val authFeature: AuthFeature by inject()
    private val profileFeature: ProfileFeature by inject()
    private val projectsFeature: ProjectsFeature by inject()
    private val sprintsFeature: SprintsFeature by inject()
    private val kardsFeature: KardsFeature by inject()
    private val projectGroupsFeature: ProjectGroupsFeature by inject()
    private val curatorshipService: CuratorshipFeature by inject()
    private val invitesFeature: InvitesFeature by inject()

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
                            Config.SslConfig.password.toString().toCharArray()
                        )
                    },
                    keyAlias = "sampleAlias",
                    keyStorePassword = { Config.SslConfig.password.toString().toCharArray() },
                    privateKeyPassword = { Config.SslConfig.password.toString().toCharArray() }
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
                allowHost("kaelesty.ru", schemes = listOf("https"))
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

                post("auth/register") {
                    authFeature.register(this)
                }

                authenticate("auth-jwt") {

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

                    get("project/kards") {
                        kardsFeature.getProjectKards(this)
                    }

                    get("/project/get") {
                        projectsFeature.getProject(this)
                    }

                    get("/project/repo/remove") {
                        projectsFeature.removeRepository(this)
                    }

                    get("/project/repo/add") {
                        projectsFeature.addRepository(this)
                    }

                    post("project/update") {
                        projectsFeature.updateProjectMeta(this)
                    }

                    post("projectgroup/create") {
                        projectGroupsFeature.createProjectsGroup(this)
                    }

                    get("/projectgroup/getCuratorGroups") {
                        projectGroupsFeature.getCuratorProjectGroups(this)
                    }

                    get("/projectgroup/getGroupProjects") {
                        projectGroupsFeature.getGroupProjects(this)
                    }

                    post("/curatorship/retrySubmission") {
                        curatorshipService.retrySubmission(this)
                    }

                    post("/curatorship/approve") {
                        curatorshipService.approveProject(this)
                    }

                    post("/curatorship/disapprove") {
                        curatorshipService.disapproveProject(this)
                    }

                    get("/curatorship/getPendingProjects") {
                        curatorshipService.getPendingProjects(this)
                    }

                    post("sprint/create") {
                        sprintsFeature.createSprint(this)
                    }

                    get("/invites/get") {
                        invitesFeature.getProjectInvite(this)
                    }

                    get("/invites/use") {
                        invitesFeature.useInvite(this)
                    }

                    post("/invites/refresh") {
                        invitesFeature.refreshProjectInvite(this)
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