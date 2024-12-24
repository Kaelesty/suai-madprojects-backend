package domain

import domain.profile.SharedProfile
import shared_domain.entities.BranchCommits
import shared_domain.entities.RepoView

interface BranchesRepo {

    suspend fun getProjectRepoBranches(
        projectId: String,
        githubJwt: String,
    ): List<RepoView>?

    suspend fun getRepoBranchContent(
        sha: String,
        repoName: String,
        githubJwt: String,
        profileMaker: suspend (Int) -> SharedProfile?,
    ): BranchCommits?

    suspend fun getCommitsCount(
        projectId: String,
        githubJwt: String,
    ): List<CommiterModel>

}

data class CommiterModel(
    val fullName: String,
    val commitsCount: Int,
)