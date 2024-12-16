package app.features.curatorship

import kotlinx.serialization.Serializable

@Serializable
data class DisapproveProjectRequest(
    val projectId: String,
    val message: String,
)