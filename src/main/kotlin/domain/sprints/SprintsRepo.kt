package domain.sprints

import app.features.sprints.CreateSprintRequest
import app.features.sprints.UpdateSprintRequest

interface SprintsRepo {

    suspend fun createSprint(
        request: CreateSprintRequest,
    ): String

    suspend fun getProjectSprints(projectId: String): List<ProfileSprint>

    suspend fun finishSprint(sprintId: String)

    suspend fun getSprintProjectId(sprintId: String): String

    suspend fun getSprint(sprintId: String): Sprint

    suspend fun updateSprint(
        request: UpdateSprintRequest
    )
}