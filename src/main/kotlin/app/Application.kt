package app

import app.features.GithubFeature
import app.features.WsFeature
import entities.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.security.KeyStore

class Application : KoinComponent {

    private val githubFeature: GithubFeature by inject()
    private val wsFeature: WsFeature by inject()

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

            install(CORS) {
                anyHost()
                allowHeader("code")
                allowHeader("state")
                allowHeader("repolink")
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

                get("/dbg") {
                    call.respond(HttpStatusCode.OK, "ouch")
                }

                get("/getUserMeta") {
                    githubFeature.getUserMeta(this)
                }

                get("/getRepoBranchContent") {
                    githubFeature.getRepoBranchContent(this)
                }

                get("/getProjectRepoBranches") {
                    githubFeature.getProjectRepoBranches(this)
                }

                get("/verifyRepoLink") {
                    githubFeature.verifyRepoLink(this)
                }

                get("/githubCallbackUrl") {
                    githubFeature.proceedGithubApiCallback(this)
                }

                webSocket("/project") {
                    wsFeature.install(this)
                }
            }
        }
    }
}