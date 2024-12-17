package data.repos

import data.schemas.CommonUsersDataService
import data.schemas.CuratorsDataService
import data.schemas.GithubService
import data.schemas.UserService
import domain.profile.CommonUser
import domain.profile.CuratorUser
import domain.profile.ProfileRepo
import domain.profile.SharedProfile

class ProfileRepoImpl(
    private val usersService: UserService,
    private val commonUsersDataService: CommonUsersDataService,
    private val curatorsDataService: CuratorsDataService,
    private val githubService: GithubService,
): ProfileRepo {

    override suspend fun getSharedById(userId: String): SharedProfile? {
        usersService.getById(userId.toInt())?.let {
            return SharedProfile(
                firstName = it.firstName,
                secondName = it.secondName,
                lastName = it.lastName
            )
        }
        return null
    }

    override suspend fun getSharedByGithubId(githubId: Int): SharedProfile? {
        githubService.getUserMeta(githubId)?.let {
            return getSharedById(it.id)
        }
        return null
    }

    override suspend fun getCuratorById(userId: String): CuratorUser? {
        val user = usersService.getById(userId.toInt())
        if (user == null) return null
        val grade = curatorsDataService.getByUser(userId.toInt())
        return CuratorUser(
            data = user,
            grade = grade ?: "null"
        )
    }

    override suspend fun getCommonById(userId: String): CommonUser? {
        val user = usersService.getById(userId.toInt())
        if (user == null) return null
        val group = commonUsersDataService.getByUser(userId.toInt())
        return CommonUser(
            data = user,
            group = group ?: "null"
        )
    }

    override suspend fun updateCommon(
        userId: String,
        firstName: String?,
        secondName: String?,
        lastName: String?,
        group: String?
    ) {
        group?.let {
            commonUsersDataService.update(userId, it)
        }
        usersService.update(
            userId, firstName, secondName, lastName
        )
    }

    override suspend fun updateCurator(
        userId: String,
        firstName: String?,
        secondName: String?,
        lastName: String?,
        grade: String?
    ) {
        grade?.let {
            curatorsDataService.update(userId.toInt(), it)
        }
        usersService.update(
            userId, firstName, secondName, lastName
        )
    }

    override suspend fun checkIsCurator(userId: String): Boolean {
        return curatorsDataService.getByUser(userId.toInt()) != null
    }
}