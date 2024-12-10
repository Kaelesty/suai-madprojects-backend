package data.repos

import domain.RepositoriesRepo
import shared_domain.entities.Repository

class RepositoriesRepoImpl: RepositoriesRepo {

    override suspend fun getProjectRepos(projectId: String): List<Repository> {
        return listOf(
            Repository(
                id = "0",
                link = "https://github.com/Kaelesty/project-audionautica",
                projectId = "0"
            )
        )
    }
}
