package shared_domain.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Branch(
    val name: String,
    @SerialName("commit") val data: BranchData
)

@Serializable
data class BranchData(
    val sha: String
)

@Serializable
data class RepoBranchView(
    val name: String,
    val sha: String,
)
