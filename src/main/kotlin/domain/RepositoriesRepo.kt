package domain

import shared_domain.entities.Repository

interface RepositoriesRepo {

    suspend fun getProjectRepos(projectId: String): List<Repository>

    suspend fun removeRepo(repoId: String)

    suspend fun addRepo(projectId: String, repoLink: String): String

    suspend fun getById(repoId: String): Repository
}