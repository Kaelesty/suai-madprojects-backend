package domain.auth

data class User(
    val id: String,
    val username: String,
    val password: String,
    val lastName: String,
    val firstName: String,
    val secondName: String,
    val email: String,
    val userType: UserType
)

data class UserContext(
    val id: String,
    val type: UserType,
)