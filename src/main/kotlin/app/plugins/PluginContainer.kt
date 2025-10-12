package app.plugins

import io.ktor.server.routing.Route

class PluginContainer(
    private val plugins: List<Plugin>
) {
    fun setupAll(route: Route) {
        plugins.forEach { it.setup(route) }
    }
}

fun Route.setupAll(pluginContainer: PluginContainer) {
    pluginContainer.setupAll(this)
}