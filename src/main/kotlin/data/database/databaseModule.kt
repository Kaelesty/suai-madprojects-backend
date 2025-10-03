package data.database

import data.database.executors.aExecutor
import data.database.executors.iExecutor
import data.database.executors.lExecutor
import org.koin.dsl.module

val databaseModule = module {

    single<iExecutor> {
        iExecutor(
            dbConnector = get(),
        )
    }

    single<lExecutor> {
        lExecutor(
            dbConnector = get(),
        )
    }

    single<aExecutor> {
        aExecutor(
            dbConnector = get(),
        )
    }
}