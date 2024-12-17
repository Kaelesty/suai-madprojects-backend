package app.features.project

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProjectMetaRequest(
    val projectId: String,
    val title: String?,
    val desc: String?,
)