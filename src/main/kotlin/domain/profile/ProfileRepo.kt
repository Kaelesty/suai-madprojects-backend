package domain.profile

interface ProfileRepo {

    suspend fun getCommonById(userId: String): CommonUser?

    suspend fun getCuratorById(userId: String): CuratorUser?

    suspend fun updateCommon(
        userId: String,
        firstName: String?,
        secondName: String?,
        lastName: String?,
        group: String?,
    )

    suspend fun updateCurator(
        userId: String,
        firstName: String?,
        secondName: String?,
        lastName: String?,
        grade: String?,
    )

    suspend fun checkIsCurator(userId: String): Boolean

    suspend fun getSharedById(userId: String): SharedProfile?

    suspend fun getSharedByGithubId(githubId: Int): SharedProfile?
}