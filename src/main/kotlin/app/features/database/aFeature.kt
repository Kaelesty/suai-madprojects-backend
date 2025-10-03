package app.features.database

import domain.database.aRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.server.response.respond

interface aFeature {
    suspend fun getProjectUsers(rc: RoutingContext)
    suspend fun getGroupProjects(rc: RoutingContext)
    suspend fun addProjectToGroup(rc: RoutingContext)
    suspend fun getAllProjectStatuses(rc: RoutingContext)
    suspend fun approveProject(rc: RoutingContext)
    suspend fun createSprintKard(rc: RoutingContext)
    suspend fun moveKard(rc: RoutingContext)
    suspend fun getGroupMarksCsv(rc: RoutingContext)
    suspend fun approveAllGroupProjects(rc: RoutingContext)
    suspend fun removeUserFromProject(rc: RoutingContext)
}

class aFeatureImpl(
    private val aRepo: aRepo,
    private val validator: KeyValidator
) : aFeature {

    override suspend fun getProjectUsers(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing projectId")

        val data = aRepo.getProjectUsers(projectId)
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getGroupProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val groupId = rc.call.parameters["groupId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing groupId")

        val data = aRepo.getGroupProjects(groupId)
        rc.call.respondText(
            text = Json.encodeToString(data),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun addProjectToGroup(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing projectId")
        val groupId = rc.call.parameters["groupId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing groupId")
        val curatorId = rc.call.parameters["curatorId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing curatorId")

        val result = aRepo.addProjectToGroup(projectId, groupId, curatorId, "pending")
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getAllProjectStatuses(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val data = aRepo.getAllProjectStatuses()
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

        val result = aRepo.approveProject(projectId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun createSprintKard(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        val name = rc.call.parameters["name"]
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing name")
        val authorId = rc.call.parameters["authorId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing authorId")
        val desc = rc.call.parameters["desc"]
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing desc")
        val sprintId = rc.call.parameters["sprintId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing sprintId")

        val result = aRepo.createSprintKard(name, authorId, desc, sprintId, createTimeMillis = System.currentTimeMillis())
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun moveKard(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        val kardId = rc.call.parameters["kardId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing kardId")
        val columnId = rc.call.parameters["columnId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing columnId")
        val newOrder = rc.call.parameters["newOrder"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing newOrder")

        val result = aRepo.moveKard(kardId, columnId, newOrder)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getGroupMarksCsv(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val groupId = rc.call.parameters["groupId"]?.toIntOrNull()
            ?: return rc.call.respond(HttpStatusCode.BadRequest, "Missing groupId")

        val data = aRepo.getGroupMarksCsv(groupId)
        rc.call.respondText(
            text = data.csvData,
            status = HttpStatusCode.OK,
        )
    }

    override suspend fun approveAllGroupProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        val groupId = rc.call.parameters["groupId"]?.toIntOrNull()

        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        if (groupId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "Missing groupId")
            return
        }

        val result = aRepo.approveAllGroupProjects(groupId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun removeUserFromProject(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        val userId = rc.call.parameters["userId"]?.toIntOrNull()
        val projectId = rc.call.parameters["projectId"]?.toIntOrNull()

        if (role != KeyValidator.AdminRole.SUPER) {
            rc.call.respond(HttpStatusCode.Forbidden)
            return
        }
        if (userId == null || projectId == null) {
            rc.call.respond(HttpStatusCode.BadRequest, "Missing parameters")
            return
        }

        val result = aRepo.removeUserFromProject(userId, projectId)
        rc.call.respondText(
            text = """{"success": $result}""",
            status = if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            contentType = ContentType.Application.Json
        )
    }
}