package app

import entities.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun test() {
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
            Intent.Kanban.Start as Intent
        )
    )


    println(
        Json.encodeToString(
            Intent.Kanban.CreateColumn(
                name = "ToDo"
            ) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.CreateKard(
                name = "Task 1",
                desc = "123",
                columnId = 1
            ) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.MoveKard(
                id = 1,
                columnId = 1,
                newPosition = 2,
                newColumnId = 1
            ) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.GetKanban as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.DeleteKard(9) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.DeleteColumn(4) as Intent
        )
    )
}