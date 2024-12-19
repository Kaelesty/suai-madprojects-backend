package data.schemas

import domain.projectgroups.ProjectGroup
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ProjectGroupService(
    database: Database
) {

    object ProjectsGroup : Table() {
        val id = integer("id").autoIncrement()
        val curatorId = integer("curator_id")
            .references(UserService.Users.id)
        val title = varchar("title", length = 64)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ProjectsGroup)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(curatorId_: Int, title_: String) = dbQuery {
        val newId = ProjectsGroup.insert {
            it[curatorId] = curatorId_
            it[title] = title_
        }[ProjectsGroup.id]
        return@dbQuery ProjectGroup(
            id = newId.toString(),
            curatorId = curatorId_.toString(),
            title = title_
        )
    }

    suspend fun getGetById(id_: Int) = dbQuery {
        ProjectsGroup.selectAll()
            .where { ProjectsGroup.id eq id_ }
            .map {
                ProjectGroup(
                    id = it[ProjectsGroup.id].toString(),
                    curatorId = it[ProjectsGroup.curatorId].toString(),
                    title = it[ProjectsGroup.title]
                )
            }
            .first()
    }

    suspend fun getCuratorProjectGroups(curatorId_: Int) = dbQuery {
        ProjectsGroup.selectAll()
            .where { ProjectsGroup.curatorId eq curatorId_ }
            .map {
                ProjectGroup(
                    id = it[ProjectsGroup.id].toString(),
                    curatorId = it[ProjectsGroup.curatorId].toString(),
                    title = it[ProjectsGroup.title]
                )
            }
    }

    suspend fun checkIsCuratorGroupOwner(curatorId_: Int, groupId_: Int) = dbQuery {
        ProjectsGroup.selectAll()
            .where { ProjectsGroup.id eq groupId_ }
            .map { it[ProjectsGroup.curatorId] }
            .first() == curatorId_
    }
}