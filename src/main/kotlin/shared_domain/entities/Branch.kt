package shared_domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class Branch(
    val name: String,
    val commit: CommitBlock,
    val protected: Boolean,
)

@Serializable
data class CommitBlock(
    val sha: String,
    val url: String,
)

@Serializable
data class RepoBranchView(
    val name: String,
    val sha: String,
    val commitsLink: String,
)