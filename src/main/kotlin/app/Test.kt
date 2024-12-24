
import entities.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {
    println(
        Json.encodeToString(
            Intent.Authorize(
                jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIzIiwiZXhwIjoxNzM0OTM4ODI0fQ.imWrUrdpdQv2JxoPg--Cab5jy12M1whiEjwsGu3jS2c"
            ) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.Start(1) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.GetKanban(
                projectId = 1
            ) as Intent
        )
    )
}
