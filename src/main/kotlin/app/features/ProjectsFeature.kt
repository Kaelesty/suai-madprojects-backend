package app.features

import domain.project.ProjectRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ProjectsFeature {

    suspend fun getCurators(rc: RoutingContext)
}

class ProjectsFeatureImpl(
    private val projectRepo: ProjectRepo,
): ProjectsFeature {

    override suspend fun getCurators(rc: RoutingContext) {
        with(rc) {
            val curators = projectRepo.getCuratorsList()
            call.respondText(
                text = Json.encodeToString(curators),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }
}