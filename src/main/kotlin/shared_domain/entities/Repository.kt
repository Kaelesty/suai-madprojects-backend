package shared_domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class Repository(
    val id: String,
    val link: String,
    val projectId: String,
)