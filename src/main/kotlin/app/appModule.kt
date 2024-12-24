package app

import app.config.Config
import app.features.featuresModule
import org.koin.dsl.module
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val appModule = module {

    includes(featuresModule)

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

    single<Config> {
        Config.load()
    }

    single<GithubTokenUtil> {
        GithubTokenUtil(
            githubTokensRepo = get(),
            httpClient = get(),
            config = get(),
        )
    }
}