
import entities.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID

fun main() {
    println(
        Json.encodeToString(
            Intent.KeepAlive as Intent
        )
    )
}
