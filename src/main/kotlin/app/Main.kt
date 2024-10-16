package app

import di.appModule
import di.dataModule
import di.domainModule
import org.koin.core.context.GlobalContext.startKoin


fun main() {

    startKoin {
        modules(appModule, domainModule, dataModule)
    }

    val application = Application()
    application.run()
}