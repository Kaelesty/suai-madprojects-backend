package app

import app.features.EmailFeature
import app.features.InvitesFeature
import app.features.KardsFeature
import app.features.MarksFeature
import app.features.WsFeature
import app.features.activity.ActivityFeature
import app.features.auth.AuthFeature
import app.features.curatorship.CuratorshipFeature
import app.features.database.aFeature
import app.features.database.iFeature
import app.features.database.lFeature
import app.features.github.GithubFeature
import app.features.profile.ProfileFeature
import app.features.project.ProjectsFeature
import app.features.projectgroups.ProjectGroupsFeature
import app.features.sprints.SprintsFeature
import app.plugins.PluginContainer
import app.plugins.setupAll
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
import io.ktor.server.response.respondFile
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
    private val marksFeature by inject<MarksFeature>()
    private val emailFeature by inject<EmailFeature>()

    private val iFeature by inject<iFeature>()
    private val lFeature by inject<lFeature>()
    private val aFeature by inject<aFeature>()

    private val pluginContainer by inject<PluginContainer>()

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

                get("/inst") {
                    call.respondFile(
                        File("src/main/resources/Inst.pdf")
                    )
                }

                post("/auth/login") {
                    authFeature.login(this)
                }

                post("/auth/register") {
                    authFeature.register(this)
                }

                post("/admin/sayHello") {
                    emailFeature.sayHello(this)
                }

                authenticate("auth-jwt") {

                    setupAll(pluginContainer)

                    get("/hello") {
                        val principal = call.principal<JWTPrincipal>()
                        val username = principal!!.payload.getClaim("username").asString()
                        val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                        call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
                    }

                    post("/auth/refresh") {
                        authFeature.refresh(this)
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

                    post("projectGroup/delete") {
                        projectGroupsFeature.deleteProjectGroup(this)
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

                    post("project/createKardChat") {
                        projectsFeature.createKardChat(this)
                    }
                }

                //swaggerFeature.install(this)

                get("/github/githubCallbackUrl") {
                    githubFeature.proceedGithubApiCallback(this)
                }

                webSocket("/project") {
                    wsFeature.install(this)
                }



                route("/db/i") {
                    // 4.1. Получить список кураторов без назначенных проектов
                    get("/getUnassignedCurators") {
                        iFeature.getUnassignedCurators(this)
                    }

                    // 4.2. Получить список проектов, которые ожидают одобрения
                    get("/getPendingApprovalProjects") {
                        iFeature.getPendingApprovalProjects(this)
                    }

                    // 4.3. Найти проекты, у которых истек срок выполнения спринта
                    get("/getProjectsWithExpiredSprints") {
                        iFeature.getProjectsWithExpiredSprints(this)
                    }

                    // 4.4. Получить список групп проектов, где все проекты уже завершены
                    get("/getGroupsWithAllProjectsCompleted") {
                        iFeature.getGroupsWithAllProjectsCompleted(this)
                    }

                    // 4.5. Найти проекты, у которых оценка ниже 4
                    get("/getProjectsWithLowMarks") {
                        iFeature.getProjectsWithLowMarks(this)
                    }

                    // 4.6. Получить группы проектов, где средняя оценка проектов ниже 4
                    get("/getGroupsWithLowAverage") {
                        iFeature.getGroupsWithLowAverage(this)
                    }

                    // 4.7. Найти проекты, в которых количество коммитов превышает среднее значение
                    get("/getProjectsWithAboveAverageCards") {
                        iFeature.getProjectsWithAboveAverageCards(this)
                    }

                    // 4.8. Найти проекты, которые получили оценку отлично
                    get("/getExcellentProjects") {
                        iFeature.getExcellentProjects(this)
                    }

                    // 4.9. Получить список пользователей, которые не участвовали в чате проекта
                    get("/getUsersWithoutChatParticipation") {
                        iFeature.getUsersWithoutChatParticipation(this)
                    }

                    // 4.10. Найти чаты, в которых больше всего сообщений
                    get("/getMostActiveChats") {
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                        iFeature.getMostActiveChats(this, limit)
                    }
                }

                route("/db/l") {
                    // Л.1 Найти всех студентов, которые не состоят ни в одном проекте
                    get("/getStudentsWithoutProjects") {
                        lFeature.getStudentsWithoutProjects(this)
                    }

                    // Л.2 Получить список проектов, не завершивших ни одного спринта
                    get("/getProjectsWithoutCompletedSprints") {
                        lFeature.getProjectsWithoutCompletedSprints(this)
                    }

                    // Л.3 Получить список всех пользователей
                    get("/getAllUsers") {
                        lFeature.getAllUsers(this)
                    }

                    // Л.4 Получить список неодобренных проектов
                    get("/getUnapprovedProjects") {
                        lFeature.getUnapprovedProjects(this)
                    }

                    // Л.5 Найти проекты, к которым не привязан репозиторий GitHub
                    get("/getProjectsWithoutRepo") {
                        lFeature.getProjectsWithoutRepo(this)
                    }

                    // Л.6 Получить группы проектов, где средняя оценка проектов ниже 4
                    get("/getLowRatingGroups") {
                        lFeature.getLowRatingGroups(this)
                    }

                    // Л.7 Найти спринты, которые не были завершены в срок
                    get("/getOverdueSprints") {
                        lFeature.getOverdueSprints(this)
                    }

                    // Л.8 Найти пользователей, которые не авторизировали GitHub
                    get("/getUsersWithoutGitHub") {
                        lFeature.getUsersWithoutGitHub(this)
                    }

                    // Л.9 Одобрить проект (Admin)
                    post("/approveProject") {
                        lFeature.approveProject(this)
                    }

                    // Л.10 Удалить проект (Admin)
                    delete("/deleteProject") {
                        lFeature.deleteProject(this)
                    }
                }

                route("/db/a") {
                    // А.1 Найти пользователей, относящихся к проекту
                    get("/getProjectUsers") {
                        aFeature.getProjectUsers(this)
                    }

                    // А.2 Найти проекты, относящиеся к группе проектов
                    get("/getGroupProjects") {
                        aFeature.getGroupProjects(this)
                    }

                    // А.3 Добавить проект к группе проектов (Admin)
                    post("/addProjectToGroup") {
                        aFeature.addProjectToGroup(this)
                    }

                    // А.4 Получить статусы всех проектов
                    get("/getAllProjectStatuses") {
                        aFeature.getAllProjectStatuses(this)
                    }

                    // А.5 Одобрить проект (Admin)
                    post("/approveProject") {
                        aFeature.approveProject(this)
                    }

                    // А.6 Создание новой карточки в спринте (Admin)
                    post("/createSprintKard") {
                        aFeature.createSprintKard(this)
                    }

                    // А.7 Передвижение карточки в спринте (Admin)
                    post("/moveKard") {
                        aFeature.moveKard(this)
                    }

                    // А.8 Получить оценки в группе проектов в формате CSV
                    get("/getGroupMarksCsv") {
                        aFeature.getGroupMarksCsv(this)
                    }

                    // А.9 Одобрить все проекты в группе (Admin)
                    post("/approveAllGroupProjects") {
                        aFeature.approveAllGroupProjects(this)
                    }

                    // А.10 Удалить пользователя из проекта (Admin)
                    delete("/removeUserFromProject") {
                        aFeature.removeUserFromProject(this)
                    }
                }
            }
        }
    }
}