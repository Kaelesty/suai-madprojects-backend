package app.features.database

import domain.database.lRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface lFeature {
    suspend fun getStudentsWithoutProjects(rc: RoutingContext)
    suspend fun getProjectsWithoutCompletedSprints(rc: RoutingContext)
    suspend fun getAllUsers(rc: RoutingContext)
    suspend fun getUnapprovedProjects(rc: RoutingContext)
    suspend fun getProjectsWithoutRepo(rc: RoutingContext)
    suspend fun getLowRatingGroups(rc: RoutingContext)
    suspend fun getOverdueSprints(rc: RoutingContext)
    suspend fun getUsersWithoutGitHub(rc: RoutingContext)
    suspend fun approveProject(rc: RoutingContext)
    suspend fun deleteProject(rc: RoutingContext)
}

class lFeatureImpl(
    private val lRepo: lRepo,
    private val validator: KeyValidator
) : lFeature {

    override suspend fun getStudentsWithoutProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getStudentsWithoutProjects()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getProjectsWithoutCompletedSprints(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getProjectsWithoutCompletedSprints()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getAllUsers(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getAllUsers()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getUnapprovedProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getUnapprovedProjects()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getProjectsWithoutRepo(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getProjectsWithoutRepo()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getLowRatingGroups(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getLowRatingGroups()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getOverdueSprints(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getOverdueSprints()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getUsersWithoutGitHub(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = lRepo.getUsersWithoutGitHub()
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun approveProject(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()

        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }

        if (projectId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "Missing projectId")
            return
        }

        val result = lRepo.approveProject(projectId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun deleteProject(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()

        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }

        if (projectId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "Missing projectId")
            return
        }

        val result = lRepo.deleteProject(projectId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }
}