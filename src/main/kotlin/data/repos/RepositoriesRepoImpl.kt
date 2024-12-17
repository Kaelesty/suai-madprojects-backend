package data.repos

import data.schemas.ProjectReposService
import domain.RepositoriesRepo
import shared_domain.entities.Repository

class RepositoriesRepoImpl(
    private val projectsReposService: ProjectReposService
): RepositoriesRepo {

    override suspend fun getById(repoId: String): Repository {
        return projectsReposService.getById(repoId.toInt()).let {
            Repository(
                id = it.first.toString(),
                link = it.second,
                projectId = ""
            )
        }
    }

    override suspend fun getProjectRepos(projectId: String): List<Repository> {
        return projectsReposService.getByProjectId(projectId.toInt()).map {
            Repository(
                id = it.first.toString(),
                link = it.second,
                projectId = projectId
            )
        }

    }

    override suspend fun removeRepo(repoId: String) {
        projectsReposService.remove(repoId.toInt())
    }

    override suspend fun addRepo(projectId: String, repoLink: String): String {
        return projectsReposService.create(
            projectId_ = projectId.toInt(), link_ = repoLink
        ).toString()
    }
}
