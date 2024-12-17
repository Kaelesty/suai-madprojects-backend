package domain.activity

interface ActivityRepo {

    suspend fun getProjectActivity(projectId: String, count: Int?): List<Activity>

    suspend fun recordActivity(
        projectId: String,
        actorId: String?,
        targetTitle: String,
        targetId: String,
        type: ActivityType,
        secondaryTargetTitle: String? = null
    )
}