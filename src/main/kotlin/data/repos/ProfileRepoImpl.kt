package data.repos

import data.schemas.CommonUsersDataService
import data.schemas.CuratorsDataService
import data.schemas.UserService
import domain.profile.CommonUser
import domain.profile.CuratorUser
import domain.profile.ProfileRepo

class ProfileRepoImpl(
    private val usersService: UserService,
    private val commonUsersDataService: CommonUsersDataService,
    private val curatorsDataService: CuratorsDataService,
): ProfileRepo {

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

    override suspend fun update(
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

    override suspend fun checkIsCurator(userId: String): Boolean {
        // TODO
        return true
    }
}