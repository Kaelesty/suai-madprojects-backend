package app.features.auth

import app.Config
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date
import kotlin.math.exp

interface AuthFeature {

    suspend fun login(rc: RoutingContext)

    fun install_(app: Application)
}

class AuthFeatureImpl : AuthFeature {

    private val tokenLifetime = 60 * 1000 * 12 * 60

    override fun install_(app: Application) {
        with(app) {
            install(Authentication) {
                jwt("auth-jwt") {
                    verifier(
                        JWT
                            .require(Algorithm.HMAC256(Config.Auth.secret))
                            .withAudience(Config.Auth.issuer)
                            .withIssuer(Config.Auth.issuer)
                            .build()
                    )
                    validate { credential ->
                        try {
                            if (credential.payload.getClaim("userId").asString() != "") {
                                JWTPrincipal(credential.payload)
                            } else {
                                null
                            }
                        }
                        catch (e: Exception) {
                            null
                        }
                    }
                    challenge { defaultScheme, realm ->
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            }
        }
    }

    override suspend fun login(rc: RoutingContext) {
        with(rc) {
            val user = call.receive<LoginRequest>()
            val userId = "234"
            val expireTime = System.currentTimeMillis() + tokenLifetime

            val token = JWT.create()
                .withAudience(Config.Auth.issuer)
                .withIssuer(Config.Auth.issuer)
                .withClaim("userId", userId)
                .withExpiresAt(Date(expireTime))
                .sign(Algorithm.HMAC256(Config.Auth.secret))
            call.respondText(
                text = Json.encodeToString(LoginResponse(token, expireTime)),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}