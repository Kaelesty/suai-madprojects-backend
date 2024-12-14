package app.features.profile

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val firstName: String?,
    val secondName: String?,
    val lastName: String?,
    val group: String?,
)

@Serializable
data class SharedProfileResponse(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val avatar: String?,
)