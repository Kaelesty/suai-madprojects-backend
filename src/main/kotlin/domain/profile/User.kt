package domain.profile

import domain.auth.User
import domain.auth.UserType
import kotlinx.serialization.Serializable


data class CommonUser(
    val data: User,
    val group: String,
)

data class CuratorUser(
    val data: User,
    val grade: String,
)

@Serializable
data class SharedProfile(
    val firstName: String,
    val secondName: String,
    val lastName: String,
)

@Serializable
data class RoledSharedProfile(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val role: UserType,
    val email: String,
)