package shared_domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class GithubTokens(
    val access_token: String,
    val refresh_token: String
)