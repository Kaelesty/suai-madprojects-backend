package shared_domain.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BranchCommit(
    val sha: String,
    @SerialName("commit") val data: CommitData,
    val author: AuthorData,
)

@Serializable
data class AuthorData(
    val id: Int,
)

@Serializable
data class CommitData(
    val author: CommitAuthor,
    val message: String,
)

@Serializable
data class CommitAuthor(
    val name: String,
    val date: String,
)

@Serializable
data class BranchCommits(
    val commits: List<BranchCommitView>,
    val authors: List<GithubUserMeta>
)

@Serializable
data class BranchCommitAuthor(
    val githubId: Int,
    val id: String,
    val avatarUrl: String,
)

@Serializable
data class BranchCommitView(
    val sha: String,
    val authorGithubId: Int,
    val date: String,
    val message: String,
)

@Serializable
data class RepoBranchResponse(
    val repos: List<RepoView>,
)

@Serializable
data class RepoView(
    val name: String,
    val repoBranches: List<RepoBranchView>,
)