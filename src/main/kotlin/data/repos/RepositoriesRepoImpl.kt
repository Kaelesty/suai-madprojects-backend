package data.repos

import data.schemas.ProjectReposService
import domain.RepositoriesRepo
import shared_domain.entities.Repository

class RepositoriesRepoImpl(
    private val projectsReposService: ProjectReposService
): RepositoriesRepo {

    override suspend fun getProjectRepos(projectId: String): List<Repository> {
        return projectsReposService.getByProjectId(projectId.toInt()).map {
            Repository(
                id = it.first.toString(),
                link = it.second,
                projectId = projectId
            )
        }

    }
}
