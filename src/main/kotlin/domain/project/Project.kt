package domain.project

import kotlinx.serialization.Serializable

@Serializable
data class ProjectMeta(
    val id: String,
    val title: String,
    val desc: String,
    val maxMembersCount: Int,
)