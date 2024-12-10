package domain

import shared_domain.entities.Repository

interface RepositoriesRepo {

    suspend fun getProjectRepos(projectId: String): List<Repository>
}