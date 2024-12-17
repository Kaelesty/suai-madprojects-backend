package data.repos

import app.features.sprints.CreateSprintRequest
import app.features.sprints.UpdateSprintRequest
import data.getCurrentDate
import data.schemas.KardInSprintService
import data.schemas.SprintsService
import domain.sprints.ProfileSprint
import domain.sprints.Sprint
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

    override suspend fun updateSprint(request: UpdateSprintRequest) {
        val sprintKards = kardsInSprintsService.getSprintKardIds(request.sprintId)
        request.kardIds
            .filter { !sprintKards.contains(it.toInt()) }
            .forEach {
                kardsInSprintsService.create(
                    kardId_ = it,
                    sprintId_ = request.sprintId
                )
            }
        sprintsService.update(request.sprintId, request.title, request.desc)
    }

    override suspend fun getProjectSprints(projectId: String): List<ProfileSprint> {
        return sprintsService.getByProject(projectId)
    }

    override suspend fun finishSprint(sprintId: String) {
        sprintsService.finishSprint(
            sprintId_ = sprintId,
            endDate_ = getCurrentDate()
        )
    }

    override suspend fun getSprintProjectId(sprintId: String): String {
        return sprintsService.getSprintProjectId(sprintId.toInt()).toString()
    }

    override suspend fun getSprint(sprintId: String): Sprint {

        val sprintMeta = sprintsService.getById(sprintId.toInt())
        val kardIds = kardsInSprintsService.getSprintKardIds(sprintId)

        return Sprint(
            meta = sprintMeta,
            kardIds = kardIds.map { it.toString() }
        )
    }
}