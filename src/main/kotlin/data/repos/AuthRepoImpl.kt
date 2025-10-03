package data.repos

import data.schemas.CommonUsersDataService
import data.schemas.CuratorsDataService
import data.schemas.RefreshService
import data.schemas.UserService
import domain.auth.AuthRepo
import domain.auth.AuthSecret
import domain.auth.CheckUniqueResult
import domain.auth.LoginResult
import domain.auth.UserContext
import domain.auth.UserType
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.UUID

class AuthRepoImpl(
    private val userService: UserService,
    private val commonUsersDataService: CommonUsersDataService,
    private val curatorsDataService: CuratorsDataService,
    private val refreshService: RefreshService,
): AuthRepo {

    override suspend fun generateRefreshRequest(userId: String, expiresAt: Long): String {
        var uuid = ""
        do {
            uuid = UUID.randomUUID().toString()
        } while (refreshService.checkUnique(uuid))

        refreshService.create(
            userId_ = userId.toInt(),
            request_ = uuid,
            expiresAt_ = expiresAt
        )

        return uuid
    }

    override suspend fun checkRefreshRequest(request: String): UserContext? {
        return refreshService.check(request)?.let {
            UserContext(
                id = it.toString(),
                type = userService.getById(it)?.userType ?: return@let null
            )
        }
    }

    override suspend fun login(email: String, password: String): LoginResult {
        val user = userService.getByEmail(email)
        if (user == null) return LoginResult.NoUser
        val requestHash = generateHash(password, email)
        if (requestHash != user.password) return LoginResult.BadPassword
        return LoginResult.Ok(user.id, user.userType)
    }

    override suspend fun createCommonProfile(
        username: String,
        lastName: String,
        firstName: String,
        secondName: String,
        group: String,
        email: String,
        password: String
    ): String {
        val newId = userService.create(
            username_ = username,
            password_ = generateHash(password, email),
            lastName_ = lastName,
            firstName_ = firstName,
            secondName_ = secondName,
            email_ = email,
            userType_ = UserType.Common
        )
        commonUsersDataService.create(
            userId_ = newId.toInt(),
            group_ = group
        )
        return newId
    }

    override suspend fun createCuratorProfile(
        username: String,
        lastName: String,
        firstName: String,
        secondName: String,
        grade: String,
        email: String,
        password: String
    ): String {
        val newId = userService.create(
            username_ = username,
            password_ = generateHash(password, email),
            lastName_ = lastName,
            firstName_ = firstName,
            secondName_ = secondName,
            email_ = email,
            userType_ = UserType.Curator
        )
        curatorsDataService.create(
            userId_ = newId.toInt(),
            grade_ = grade
        )
        return newId
    }

    override suspend fun checkUnique(email: String, username: String): CheckUniqueResult {
        if (!userService.isEmailUnique(email)) return CheckUniqueResult.BadEmail
        if (!userService.isUsernameUnique(username)) return CheckUniqueResult.BadUsername
        return CheckUniqueResult.Ok
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun generateHash(value: String, salt: String): String {
        val combinedSalt = "$salt${AuthSecret.SECRET}".toByteArray()
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance(AuthSecret.ALGORITHM)
        val spec: KeySpec = PBEKeySpec(value.toCharArray(), combinedSalt, AuthSecret.ITERATIONS, AuthSecret.KEY_LENGTH)
        val key: SecretKey = factory.generateSecret(spec)
        val hash: ByteArray = key.encoded
        return hash.toHexString()
    }
}