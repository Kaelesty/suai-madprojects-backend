package data.repos

import data.schemas.UserService
import domain.project.AvailableCurator
import domain.project.ProjectRepo

class ProjectRepoImpl(
    private val userService: UserService
): ProjectRepo {

    override suspend fun getCuratorsList(): List<AvailableCurator> {
        return userService.getCurators()
    }
}