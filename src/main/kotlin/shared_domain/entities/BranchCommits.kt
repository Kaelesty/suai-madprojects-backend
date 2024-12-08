package shared_domain.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BranchCommits(
    val sha: String,
    @SerialName("node_id") val nodeId: String,
    @SerialName("commit") val commits: List<BranchCommit>
)

@Serializable
data class BranchCommit(
    val author: CommitAuthor
)

@Serializable
data class CommitAuthor(
    val name: String,

)