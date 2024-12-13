package domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val lastName: String,
    val firstName: String,
    val secondName: String,
    val data: String, // group for common, grade for curator
    val email: String,
    val password: String,
    val userType: UserType,
)