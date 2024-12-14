package app.features.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyResponse(
    @SerialName("private") val isPrivate: Boolean
)