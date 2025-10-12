package app.plugins

import app.plugins.analytics.AnalyticsPlugin
import app.plugins.analytics.ExcelWizard
import app.plugins.analytics.PoiExcelWizard
import org.koin.dsl.module

val pluginsModule = module {

    single<PluginContainer> {
        PluginContainer(plugins = getAll())
    }

    single<Plugin> {
        AnalyticsPlugin(
            projectRepo = get(),
            projectGroupsRepo = get(),
            branchesRepo = get(),
            tokenUtil = get(),
            excelWizard = get()
        )
    }

    single<ExcelWizard> {
        PoiExcelWizard()
    }
}