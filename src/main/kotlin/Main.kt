package org.example

import domain.ClientAction
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json


fun main() {
    embeddedServer(Netty, port = 8080) {
        install(WebSockets)
        routing {
            webSocket("/echo") {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    try {
                        Json.decodeFromString<ClientAction>(receivedText)
                        handleClientAction(Json.decodeFromString<ClientAction>(receivedText))
                    }
                    catch (e: Exception) {
                        send(e.message.toString())
                    }
                }
            }
        }
    }.start(wait = true)
}

fun handleClientAction(action: ClientAction) {
    when (action) {
        is ClientAction.SendMessage -> {
            val userId = getUserIdFromJWT(action.jwt)
        }
        is ClientAction.StartTyping -> {
            val userId = getUserIdFromJWT(action.jwt)
        }
    }
}

fun getUserIdFromJWT(jwt: String): Int {
    return jwt.toInt()
}