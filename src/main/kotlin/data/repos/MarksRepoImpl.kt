package data.repos

import data.schemas.ProjectCuratorshipService
import domain.MarksRepo

class MarksRepoImpl(
    private val curatorshipService: ProjectCuratorshipService
): MarksRepo {

    override suspend fun markProject(projectId: String, mark: Int) {
        curatorshipService.setMark(projectId.toInt(), mark)
    }

    override suspend fun getProjectMark(projectId: String): Int? {
        return curatorshipService.getMark(projectId.toInt())
    }
}