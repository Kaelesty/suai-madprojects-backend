package domain.sprints

interface SprintsRepo {

    suspend fun createSprint(
        request: CreateSprintRequest,
    ): String
}