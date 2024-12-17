package data.schemas

import domain.sprints.ProfileSprint
import domain.sprints.SprintMeta
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class SprintsService(
    database: Database
) {

    object Sprints : Table("sprints_") {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)
        val title = varchar("title", length = 25)
        val desc = varchar("desc", length = 1000)
        val startDate = varchar("startDate", length = 10)
        val supposedEndDate = varchar("supposedEndDate", length = 10)
        val actualEndDate = varchar("actualEndDate", length = 10)
            .nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Sprints)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(
        projectId_: String,
        title_: String,
        desc_: String,
        supposedEndDate_: String,
        startDate_: String
    ) = dbQuery {
        val newId = Sprints.insert {
            it[projectId] = projectId_.toInt()
            it[title] = title_
            it[desc] = desc_
            it[supposedEndDate] = supposedEndDate_
            it[startDate] = startDate_
        }[Sprints.id]
        return@dbQuery newId
    }

    suspend fun update(sprintId_: String, title_: String, desc_: String) = dbQuery {
        Sprints.update(
            where = { Sprints.id eq sprintId_.toInt() }
        ) {
            it[title] = title_
            it[desc] = desc_
        }
    }

    suspend fun getByProject(projectId_: String) = dbQuery {
        Sprints.selectAll()
            .where { Sprints.projectId eq projectId_.toInt() }
            .map {
                ProfileSprint(
                    startDate = it[Sprints.startDate],
                    actualEndDate = it[Sprints.actualEndDate],
                    title = it[Sprints.title],
                    id = it[Sprints.id].toString(),
                    supposedEndDate = it[Sprints.supposedEndDate]
                )
            }
    }

    suspend fun finishSprint(sprintId_: String, endDate_: String) = dbQuery {
        Sprints.update(
            where = { Sprints.id eq sprintId_.toInt() }
        ) {
            it[actualEndDate] = endDate_
        }
    }

    suspend fun getSprintProjectId(sprintId_: Int) = dbQuery {
        Sprints.selectAll()
            .where { Sprints.id eq sprintId_ }
            .map {
                it[Sprints.projectId]
            }
            .first()
    }

    suspend fun getById(sprintId_: Int) = dbQuery {
        Sprints.selectAll()
            .where { Sprints.id eq sprintId_ }
            .map {
                SprintMeta(
                    id = it[Sprints.projectId].toString(),
                    startDate = it[Sprints.startDate],
                    actualEndDate = it[Sprints.actualEndDate],
                    supposedEndDate = it[Sprints.supposedEndDate],
                    title = it[Sprints.title],
                    desc = it[Sprints.desc],

                )
            }
            .first()
    }
}