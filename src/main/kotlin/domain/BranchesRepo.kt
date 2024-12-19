package domain

import domain.profile.SharedProfile
import shared_domain.entities.BranchCommits
import shared_domain.entities.RepoView

interface BranchesRepo {

    suspend fun getProjectRepoBranches(
        projectId: String,
        githubJwt: String,
        forceInvalidateCaches: Boolean = true,
    ): List<RepoView>?

    suspend fun getRepoBranchContent(
        sha: String,
        repoName: String,
        githubJwt: String,
        profileMaker: suspend (Int) -> SharedProfile?,
        forceInvalidateCaches: Boolean = true,
    ): BranchCommits?

}