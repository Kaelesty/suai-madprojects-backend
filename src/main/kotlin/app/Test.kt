package app

import entities.ChatType
import entities.ClientAction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun test() {
    println(
        Json.encodeToString(
            ClientAction.Authorize(jwt="1", projectId = 1) as ClientAction
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

    println(
        Json.encodeToString(
            ClientAction.SendMessage(
                chatId = 0,
                message = "Aboba"
            ) as ClientAction
        )
    )

    println(
        Json.encodeToString(
            ClientAction.RequestChatMessages(
                chatId = 1
            ) as ClientAction
        )
    )

    println(
        Json.encodeToString(
            ClientAction.ReadMessage(
                messageId = 101,
                chatId = 1
            ) as ClientAction
        )
    )

    println(
        Json.encodeToString(
            ClientAction.ReadMessagesBefore(
                messageId = 150,
                chatId = 1,
            ) as ClientAction
        )
    )
}