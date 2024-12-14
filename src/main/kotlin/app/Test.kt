import domain.auth.RegisterRequest
import domain.auth.UserType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {
    print(
        Json.encodeToString(
            RegisterRequest(
                username = "kaelesty",
                lastName = "Бунделев",
                firstName = "Илья",
                secondName = "Алексеевич",
                data = "4215",
                email = "kaelesty@gmail.com",
                password = "12345678sA_!",
                userType = UserType.Curator
            )
        )
    )
}