package app

import app.features.GithubFeature
import app.features.GithubFeatureImpl
import app.features.WsFeature
import app.features.WsFeatureImpl
import org.koin.dsl.module
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val appModule = module {

    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    single<GithubFeature> {
        GithubFeatureImpl(
            integrationRepo = get(),
            githubTokensRepo = get(),
            repositoriesRepo = get(),
            httpClient = get(),
        )
    }

    single<WsFeature> {
        WsFeatureImpl(
            integrationRepo = get(),
            kanbanRepository = get(),
            messagesRepo = get(),
            unreadMessagesRepo = get(),
            chatsRepo = get(),
        )
    }
}