package app.features.database

import domain.database.iRepo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface iFeature {
    suspend fun getUnassignedCurators(rc: RoutingContext)
    suspend fun getPendingApprovalProjects(rc: RoutingContext)
    suspend fun getProjectsWithExpiredSprints(rc: RoutingContext)
    suspend fun getGroupsWithAllProjectsCompleted(rc: RoutingContext)
    suspend fun getProjectsWithLowMarks(rc: RoutingContext)
    suspend fun getGroupsWithLowAverage(rc: RoutingContext)
    suspend fun getProjectsWithAboveAverageCards(rc: RoutingContext)
    suspend fun getExcellentProjects(rc: RoutingContext)
    suspend fun getUsersWithoutChatParticipation(rc: RoutingContext)
    suspend fun getMostActiveChats(rc: RoutingContext, limit: Int = 10)
}

class iFeatureImpl(
    private val iRepo: iRepo,
    private val validator: KeyValidator,
) : iFeature {

    override suspend fun getUnassignedCurators(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val curators = iRepo.getUnassignedCurators()
        rc.call.respondText(
            text = Json.encodeToString(curators),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getPendingApprovalProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getPendingApprovalProjects()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getProjectsWithExpiredSprints(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getProjectsWithExpiredSprints()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getGroupsWithAllProjectsCompleted(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val groups = iRepo.getGroupsWithAllProjectsCompleted()
        rc.call.respondText(
            text = Json.encodeToString(groups),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getProjectsWithLowMarks(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getProjectsWithLowMarks()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getGroupsWithLowAverage(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val groups = iRepo.getGroupsWithLowAverage()
        rc.call.respondText(
            text = Json.encodeToString(groups),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getProjectsWithAboveAverageCards(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getProjectsWithAboveAverageCards()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getExcellentProjects(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val projects = iRepo.getExcellentProjects()
        rc.call.respondText(
            text = Json.encodeToString(projects),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getUsersWithoutChatParticipation(rc: RoutingContext) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val users = iRepo.getUsersWithoutChatParticipation()
        rc.call.respondText(
            text = Json.encodeToString(users),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    override suspend fun getMostActiveChats(rc: RoutingContext, limit: Int) {
        val role = rc.call.parameters["key"]?.let { validator.validate(it) }
        if (role == null) {
            rc.call.respond(HttpStatusCode.Unauthorized)
        }
        val chats = iRepo.getMostActiveChats(limit)
        rc.call.respondText(
            text = Json.encodeToString(chats),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }
}