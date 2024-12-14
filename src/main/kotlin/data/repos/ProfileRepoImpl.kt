package data.repos

import data.schemas.CommonUsersDataService
import data.schemas.CuratorsDataService
import data.schemas.GithubService
import data.schemas.UserService
import domain.auth.User
import domain.profile.CommonProfileResponse
import domain.profile.CommonUser
import domain.profile.ProfileRepo

class ProfileRepoImpl(
    private val usersService: UserService,
    private val commonUsersDataService: CommonUsersDataService,
    private val curatorsDataService: CuratorsDataService,
): ProfileRepo {

    override suspend fun getCommonById(userId: String): CommonUser? {
        val user = usersService.getById(userId.toInt())
        if (user == null) return null
        val group = commonUsersDataService.getByUser(userId.toInt())
        return CommonUser(
            data = user,
            group = group ?: "null"
        )
    }
}