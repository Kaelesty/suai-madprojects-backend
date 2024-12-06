package app

import entities.ChatType
import entities.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun getProjectId(text: String): Int {
    if (text.contains("entities.Intent.Authorize")) return -1
    val part = text.split("projectId")
    var num = ""
    var addFlag = true
    part[1].forEachIndexed { index, it ->
        if (index != 0 && index != 1 && addFlag) {
            if (it in "1234567890") {
                num += it
            } else addFlag = false
        }
    }
    if (num == "") {
        throw Exception()
    }
    return num.toInt()
}

fun main() {
    println( getProjectId("{\"type\":\"entities.Intent.Messenger.RequestChatMessages\",\"projectId\":14324,\"chatId\":1}"))
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
            Intent.Kanban.CreateKard(
                name = "Aboba",
                columnId = 1,
                desc = "123",
                projectId = 1
            ) as Intent
        )
    )



    println(
        Json.encodeToString(
            Intent.Messenger.RequestChatMessages(
                chatId = 1,
                projectId = 1,
            ) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Messenger.SendMessage(
                chatId = 1,
                projectId = 1,
                message = "aboba"
            ) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Kanban.GetKanban(
                projectId = 1
            ) as Intent
        )
    )

    println(
        Json.encodeToString(
            Intent.Messenger.CreateChat(
                projectId = 1,
                chatType = ChatType.Public,
                chatTitle = "123"
            )
        )
    )
}