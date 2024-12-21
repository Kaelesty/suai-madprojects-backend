package data.repos.branches

import shared_domain.entities.RepoBranchView

data class ProjectCachedState(
    val id: String,
    val repoBranches: List<RepoBranchCachedState>
)

data class RepoBranchCachedState(
    val view: RepoBranchView,
    val commitsCount: Int?
)