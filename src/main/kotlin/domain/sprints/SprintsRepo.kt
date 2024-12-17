package domain.sprints

import app.features.sprints.CreateSprintRequest

interface SprintsRepo {

    suspend fun createSprint(
        request: CreateSprintRequest,
    ): String
}