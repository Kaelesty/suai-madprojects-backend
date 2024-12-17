package data.repos

import app.features.sprints.CreateSprintRequest
import data.getCurrentDate
import data.schemas.KardInSprintService
import data.schemas.SprintsService
import domain.sprints.SprintsRepo

class SprintsRepoImpl(
    private val sprintsService: SprintsService,
    private val kardsInSprintsService: KardInSprintService
): SprintsRepo {

    override suspend fun createSprint(request: CreateSprintRequest): String {
        val sprintId = sprintsService.create(
            projectId_ = request.projectId,
            title_ = request.title,
            desc_ = request.desc,
            supposedEndDate_ = request.endDate,
            startDate_ = getCurrentDate()
        ).toString()

        request.kardIds.forEach {
            kardsInSprintsService.create(
                kardId_ = it,
                sprintId_ = sprintId
            )
        }

        return sprintId
    }
}