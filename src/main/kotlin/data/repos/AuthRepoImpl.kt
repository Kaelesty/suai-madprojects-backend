package data.repos

import data.schemas.CommonUsersDataService
import data.schemas.CuratorsDataService
import data.schemas.UserService
import domain.auth.AuthRepo
import domain.auth.AuthSecret
import domain.auth.CheckUniqueResult
import domain.auth.LoginResult
import domain.auth.UserType
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AuthRepoImpl(
    private val userService: UserService,
    private val commonUsersDataService: CommonUsersDataService,
    private val curatorsDataService: CuratorsDataService,
): AuthRepo {

    override suspend fun login(email: String, password: String): LoginResult {
        val user = userService.getByEmail(email)
        if (user == null) return LoginResult.NoUser
        val requestHash = generateHash(password, email)
        if (requestHash != user.password) return LoginResult.BadPassword
        return LoginResult.Ok(user.id)
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
            userType_ = UserType.Common
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
    fun generateHash(password: String, salt: String): String {
        val combinedSalt = "$salt${AuthSecret.SECRET}".toByteArray()
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance(AuthSecret.ALGORITHM)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), combinedSalt, AuthSecret.ITERATIONS, AuthSecret.KEY_LENGTH)
        val key: SecretKey = factory.generateSecret(spec)
        val hash: ByteArray = key.encoded
        return hash.toHexString()
    }
}