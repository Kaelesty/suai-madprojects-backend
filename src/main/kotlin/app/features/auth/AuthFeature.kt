package app.features.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import domain.auth.AuthRepo
import domain.auth.CheckUniqueResult
import domain.auth.LoginResult
import domain.auth.RegisterRequest
import domain.auth.UserType
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

interface AuthFeature {

    suspend fun login(rc: RoutingContext)

    suspend fun register(rc: RoutingContext)

    fun install_(app: Application)
}

class AuthFeatureImpl(
    private val authRepo: AuthRepo,
    private val jwt: JWTVerifier,
    private val config: app.config.Config
) : AuthFeature {

    private val tokenLifetime = 60 * 1000 * 12 * 60


    override fun install_(app: Application) {
        with(app) {
            install(Authentication) {
                jwt("auth-jwt") {
                    verifier(
                        jwt
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
                        print(realm)
                        print(defaultScheme.toString())
                    }
                }
            }
        }
    }

    override suspend fun register(rc: RoutingContext) {
        with(rc) {
            val request = call.receive<RegisterRequest>()

            val isUnique = authRepo.checkUnique(request.email, request.username)
            if (isUnique == CheckUniqueResult.BadEmail) {
                call.respond(HttpStatusCode.Conflict)
                return
            }
            if (isUnique == CheckUniqueResult.BadUsername) {
                call.respond(HttpStatusCode.NotAcceptable)
                return
            }

            if (!checkPassword(request.password)) {
                call.respond(HttpStatusCode.Forbidden)
                return
            }

            val userId = when(request.userType) {
                UserType.Common -> authRepo.createCommonProfile(
                    username = request.username,
                    lastName = request.lastName,
                    firstName = request.firstName,
                    secondName = request.secondName,
                    group = request.data,
                    email = request.email,
                    password = request.password
                )
                UserType.Curator -> authRepo.createCuratorProfile(
                    username = request.username,
                    lastName = request.lastName,
                    firstName = request.firstName,
                    secondName = request.secondName,
                    grade = request.data,
                    email = request.email,
                    password = request.password
                )
            }
            val expireTime = System.currentTimeMillis() + tokenLifetime
            val token = JWT.create()
                .withAudience(config.ssl.domain)
                .withIssuer(config.ssl.domain)
                .withClaim("userId", userId)
                .withExpiresAt(Date(expireTime))
                .sign(Algorithm.HMAC256(config.auth.jwtSecret))
            call.respondText(
                text = Json.encodeToString(AuthorizedResponse(token, expireTime, request.userType)),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    override suspend fun login(rc: RoutingContext) {
        with(rc) {
            val user = call.receive<LoginRequest>()
            val result = authRepo.login(user.email, user.password)
            when (result) {
                LoginResult.BadPassword -> {
                    call.respond(HttpStatusCode.Forbidden)
                }
                LoginResult.NoUser -> {
                    call.respond(HttpStatusCode.Forbidden)
                }
                is LoginResult.Ok -> {
                    val expireTime = System.currentTimeMillis() + tokenLifetime

                    val token = JWT.create()
                        .withAudience(config.ssl.domain)
                        .withIssuer(config.ssl.domain)
                        .withClaim("userId", result.userId)
                        .withExpiresAt(Date(expireTime))
                        .sign(Algorithm.HMAC256(config.auth.jwtSecret))
                    call.respondText(
                        text = Json.encodeToString(AuthorizedResponse(token, expireTime, result.userType)),
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                }
            }
        }
    }

    private fun checkPassword(password: String): Boolean {
        if (password.length < 10) return false
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return hasLowerCase && hasUpperCase && hasSpecialChar
    }
}