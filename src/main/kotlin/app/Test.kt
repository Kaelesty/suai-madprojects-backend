package app

import entities.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {
    println(
        Json.encodeToString(
            Intent.Authorize(jwt="1") as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.Start(projectId = 1) as Intent
        )
    )


    println(
        Json.encodeToString(
            Intent.Kanban.CreateColumn(
                name = "Aboba",
                projectId = 1
            ) as Intent
        )
    )
}