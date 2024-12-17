package data.repos

import data.schemas.ActivityService
import domain.activity.Activity
import domain.activity.ActivityRepo
import domain.activity.ActivityType

class ActivityRepoImpl(
    private val activityService: ActivityService
): ActivityRepo {

    override suspend fun getProjectActivity(projectId: String, count: Int?): List<Activity> {
        var activities = activityService.getByProject(projectId.toInt())

        if (count != null) {
            activities = activities.takeLast(count)
        }

        return activities
    }

    override suspend fun recordActivity(
        projectId: String,
        actorId: String?,
        targetTitle: String,
        targetId: String,
        type: ActivityType,
        secondaryTargetTitle: String?
    ) {
        activityService.create(
            projectId_ = projectId.toInt(),
            type_ = type,
            targetTitle_ = targetTitle,
            targetId_ = targetId.toInt(),
            actorId_ = actorId?.toInt(),
            secondaryTargetTitle_ = secondaryTargetTitle
        )
    }
}