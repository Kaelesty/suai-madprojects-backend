package shared_domain.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubUser(
    val id: Int,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("html_url") val profileLink: String,
)