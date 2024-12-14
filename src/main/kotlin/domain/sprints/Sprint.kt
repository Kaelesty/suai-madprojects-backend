package domain.sprints

import kotlinx.serialization.Serializable

@Serializable
data class ProfileSprint(
    val startDate: String,
    val actualEndDate: String?,
    val title: String,
    val id: String
)
