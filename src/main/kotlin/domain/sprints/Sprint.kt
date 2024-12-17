package domain.sprints

import kotlinx.serialization.Serializable

@Serializable
data class ProfileSprint(
    val id: String,
    val startDate: String,
    val actualEndDate: String?,
    val supposedEndDate: String,
    val title: String,
)

@Serializable
data class Sprint(
    val meta: SprintMeta,
    val kardIds: List<String>,
)

@Serializable
data class SprintMeta(
    val id: String,
    val startDate: String,
    val actualEndDate: String?,
    val supposedEndDate: String,
    val title: String,
    val desc: String,
)