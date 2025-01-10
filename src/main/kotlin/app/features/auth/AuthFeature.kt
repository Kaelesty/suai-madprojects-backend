package app.features.auth

import app.config.Config
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
import io.ktor.server.auth.principal
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

    suspend fun refresh(rc: RoutingContext)

    fun install_(app: Application)
}

class AuthFeatureImpl(
    private val authRepo: AuthRepo,
    private val jwt: JWTVerifier,
    private val config: Config
) : AuthFeature {

    private val accessTokenLifetime = 1000 * 60 * 30
    private val refreshTokenLifetime = 1000 * 60 * 60 * 24 * 30

    override suspend fun refresh(rc: RoutingContext) {
        with(rc) {
            val principal = call.principal<JWTPrincipal>()
            val request = principal!!.payload.getClaim("request").asString()

            val user = authRepo.checkRefreshRequest(request)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return
            }

            val tokens = generateTokens(user.id)

            call.respondText(
                text = Json.encodeToString(
                    AuthorizedResponse(
                        refreshToken = tokens.second,
                        accessToken = tokens.first,
                        userType = user.type
                    )
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    override fun install_(app: Application) {
        with(app) {
            install(Authentication) {
                jwt("auth-jwt") {
                    verifier(jwt)
                    validate { credential ->
                        try {
                            val userId = credential.payload.getClaim("userId")
                            val request = credential.payload.getClaim("request")

                            if (!userId.isMissing || !request.isMissing) {
                                JWTPrincipal(credential.payload)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            println("JWT validation error: ${e.message}")
                            null
                        }
                    }
                    challenge { defaultScheme, realm ->
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or expired JWT token.")
                        println("Realm: $realm")
                        println("Default scheme: $defaultScheme")
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
            
            val tokens = generateTokens(userId)
            
            call.respondText(
                text = Json.encodeToString(
                    AuthorizedResponse(
                        refreshToken = tokens.second,
                        accessToken = tokens.first,
                        userType = request.userType
                    )
                ),
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

                    val tokens = generateTokens(result.userId) 
                        
                    call.respondText(
                        text = Json.encodeToString(
                            AuthorizedResponse(
                                refreshToken = tokens.second,
                                accessToken = tokens.first,
                                userType = result.userType
                            )
                        ),
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
    
    private suspend fun generateTokens(userId: String): Pair<String, String> {

        val accessExpireTime = System.currentTimeMillis() + accessTokenLifetime
        val accessToken = JWT.create()
            .withClaim("userId", userId)
            .withExpiresAt(Date(accessExpireTime))
            .sign(Algorithm.HMAC256(config.auth.jwtSecret))


        val refreshExpireTime = System.currentTimeMillis() + refreshTokenLifetime
        val refreshRequest = authRepo.generateRefreshRequest(userId, refreshExpireTime)
        val refreshToken = JWT.create()
            .withClaim("request", refreshRequest)
            .withExpiresAt(Date(accessExpireTime))
            .sign(Algorithm.HMAC256(config.auth.jwtSecret))

        return accessToken to refreshToken
    }
}