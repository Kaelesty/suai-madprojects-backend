package domain.project

import kotlinx.serialization.Serializable

@Serializable
data class AvailableCurator(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val id: String,
    val username: String,
)