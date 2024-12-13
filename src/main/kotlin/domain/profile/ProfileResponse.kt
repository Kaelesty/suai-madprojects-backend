package domain.profile

import kotlinx.serialization.Serializable
import shared_domain.entities.GithubUserMeta

@Serializable
data class CommonProfileResponse(
    val firstName: String,
    val lastName: String,
    val secondName: String,
    val email: String,
    val projects: List<ProfileProject>,
    val githubMeta: GithubUserMeta?,
    val group: String,
)

@Serializable
data class ProfileProject(
    val id: String,
    val title: String,
)