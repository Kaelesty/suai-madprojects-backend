import domain.auth.RegisterRequest
import domain.auth.UserType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun isValidPassword(password: String): Boolean {
    // Регулярное выражение для проверки пароля
    val regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    return regex.contains(password)
}

fun main() {
    print(
        isValidPassword("12345678sA_!")
    )
}