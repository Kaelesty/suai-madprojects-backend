package shared_domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class GithubUserMeta(
    val githubId: Int,
    val githubAvatar: String,
    val profileLink: String,
    val id: String
)
