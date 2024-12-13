package app.features.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthorizedResponse(
    val token: String,
    val expiresAt: Long
)