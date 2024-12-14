package data.repos

import domain.sprints.CreateSprintRequest
import domain.sprints.SprintsRepo

class SprintsRepoImpl: SprintsRepo {

    override suspend fun createSprint(request: CreateSprintRequest): String {
        TODO()
    }
}