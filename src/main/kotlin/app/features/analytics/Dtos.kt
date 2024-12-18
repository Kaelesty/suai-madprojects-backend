package app.features.analytics

import kotlinx.serialization.Serializable

@Serializable
data class ProjectCommits(
    val projectId: String,
    val count: Int,
    val projectName: String,
)