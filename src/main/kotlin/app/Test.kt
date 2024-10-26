package app

import entities.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {
    println(
        Json.encodeToString(
            Intent.Authorize(jwt="1", projectId = 1) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Messenger.SendMessage(
                message = "Aboba",
                chatId = 2
            ) as Intent
        )
    )


    println(
        Json.encodeToString(
            Intent.Kanban.MoveKard(
                id = 1,
                newPosition = 4,
                newColumnId = 6,
            ) as Intent
        )
    )
}