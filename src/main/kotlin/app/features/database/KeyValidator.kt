package app.features.database

import java.security.MessageDigest

class KeyValidator {

    private val viewerHash = "8cbbe482d35db7f3c861abbd3ca8e7698a6445a42db01998652cd14bc120d2fd"
    private val superHash = "3004b4a763d969b94148c6ee552cd4fea5597c55968522f2b4c17e1ab44f9f12"

    fun validate(string: String): AdminRole? {
        return when (val hash = sha256(string)) {
            viewerHash -> AdminRole.VIEWER
            superHash -> AdminRole.SUPER
            else -> null
        }
    }

    fun sha256(string: String): String {
        return hashString(string, "SHA-256")
    }

    private fun hashString(input: String, algorithm: String): String {
        return MessageDigest
            .getInstance(algorithm)
            .digest(input.toByteArray())
            .fold("", { str, it -> str + "%02x".format(it) })
    }

    enum class AdminRole {
        VIEWER, SUPER
    }
}