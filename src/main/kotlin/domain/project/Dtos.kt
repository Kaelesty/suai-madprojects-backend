package domain.project

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(
    val title: String,
    val maxMembersCount: Int,
    val desc: String,
    val curatorId: String,
    val repoLinks: List<String>
)