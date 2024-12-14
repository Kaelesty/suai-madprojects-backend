package app.features

import io.ktor.server.routing.Routing
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.swaggerUI

interface SwaggerFeature {

    fun install(routing: Routing)
}

class SwaggerFeatureImpl: SwaggerFeature {

    override fun install(routing: Routing) {
        with(routing) {
            openAPI(path="openapi", swaggerFile = "openapi/documentation.yaml")
            //swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        }
    }
}