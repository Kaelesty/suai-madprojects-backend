package app

import entities.ChatType
import entities.ClientAction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {

    println(
        Json.encodeToString(
            ClientAction.Authorize(jwt="123", projectId = 1) as ClientAction
        )
    )

    println(
        Json.encodeToString(
            ClientAction.RequestChatsList(
                projectId = 1
            )  as ClientAction
        )
    )

    println(
        Json.encodeToString(
            ClientAction.CreateChat(
                projectId = 1,
                chatTitle = "Test chat",
                chatType = ChatType.Public
            )  as ClientAction
        )
    )

    println(
        Json.encodeToString(
            ClientAction.RequestChatsList(
                projectId = 1
            ) as ClientAction
        )
    )
}