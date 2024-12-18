package domain.commits

interface CommitsRepo {

    suspend fun updateProjectCommits(
        projectId: String,
        count: Int
    )

    suspend fun getProjectCommits(projectId: String): Int
}