package app.features.sprints

import kotlinx.serialization.Serializable

@Serializable
data class CreateSprintRequest(
    val projectId: String,
    val title: String,
    val desc: String,
    val endDate: String,
    val kardIds: List<String>,
)