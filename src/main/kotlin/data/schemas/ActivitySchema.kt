package data.schemas

import domain.activity.ActivityType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ActivityService(
    database: Database
) {

    object Activity : Table() {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)
        val type = enumerationByName<ActivityType>("type", 32)
        val targetTitle = varchar("target-title", 128)
        val targetId = integer("target-id")
        val timeMillis = long("timeMillis")
        val actorId = integer("actorId")
            .references(UserService.Users.id)
            .nullable()
        val secondaryTargetTitle = varchar("secondary-target-title", 128)
            .nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Activity)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(
        projectId_: Int,
        type_: ActivityType,
        targetTitle_: String,
        targetId_: Int,
        actorId_: Int?,
        secondaryTargetTitle_: String?,
    ) = dbQuery {
        val newId = Activity.insert {
            it[projectId] = projectId_
            it[type] = type_
            it[targetTitle] = targetTitle_
            it[targetId] = targetId_
            it[timeMillis] = System.currentTimeMillis()
            it[actorId] = actorId_
            it[secondaryTargetTitle] = secondaryTargetTitle_
        }[Activity.id]
        return@dbQuery newId
    }

    suspend fun getByProject(projectId_: Int) = dbQuery {
        Activity.selectAll()
            .where { Activity.projectId eq projectId_ }
            .map {
                domain.activity.Activity(
                    type = it[Activity.type],
                    timeMillis = it[Activity.timeMillis],
                    actorId = it[Activity.actorId].toString(),
                    targetTitle = it[Activity.targetTitle],
                    targetId = it[Activity.targetId].toString(),
                    secondaryTargetTitle = it[Activity.secondaryTargetTitle]
                )
            }
    }
}