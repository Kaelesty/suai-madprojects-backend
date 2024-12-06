package di

import org.koin.dsl.module
import app.Application
import io.ktor.client.*
import io.ktor.client.engine.cio.*

val appModule = module {

    single<HttpClient> {
        HttpClient(CIO)
    }
}