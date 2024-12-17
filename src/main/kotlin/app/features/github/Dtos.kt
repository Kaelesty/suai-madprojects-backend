package app.features.github

import domain.profile.SharedProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import shared_domain.entities.GithubUserMeta

@Serializable
data class VerifyResponse(
    @SerialName("private") val isPrivate: Boolean
)

@Serializable
data class Commiter(
    val profile: SharedProfile?,
    val githubMeta: GithubUserMeta?,
)