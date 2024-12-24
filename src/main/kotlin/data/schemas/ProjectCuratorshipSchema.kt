package data.schemas

import domain.project.ProjectStatus
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ProjectCuratorshipService(
    database: Database
) {

    object ProjectsCuratorship : Table() {
        val id = integer("id").autoIncrement()
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)
        val userId = integer("user_id")
            .references(UserService.Users.id)
        val status = enumerationByName<ProjectStatus>("status", 16)
        val projectGroupId = integer("project_group_id")
            .references(ProjectGroupService.ProjectsGroup.id)
        val mark = integer("mark")
            .nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ProjectsCuratorship)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(projectId_: String, userId_: String, projectGroupId_: String) = dbQuery {
        ProjectsCuratorship.insert {
            it[projectId] = projectId_.toInt()
            it[userId] = userId_.toInt()
            it[status] = ProjectStatus.Pending
            it[projectGroupId] = projectGroupId_.toInt()
        }
    }

    suspend fun getProjectGroupId(projectId_: Int) = dbQuery {
        ProjectsCuratorship.selectAll()
            .where { ProjectsCuratorship.projectId eq projectId_ }
            .map {
                it[ProjectsCuratorship.projectGroupId]
            }
    }

    suspend fun getProjectCurator(projectId_: Int) = dbQuery {
        ProjectsCuratorship.selectAll()
            .where { ProjectsCuratorship.projectId eq projectId_ }
            .map {
                it[ProjectsCuratorship.userId]
            }
    }

    suspend fun setStatus(projectId_: String, userId_: String, status_: ProjectStatus) = dbQuery {
        ProjectsCuratorship.update(
            where = { (ProjectsCuratorship.projectId eq projectId_.toInt()) and (ProjectsCuratorship.userId eq userId_.toInt()) }
        ) {
            it[status] = status_
        }
    }

    suspend fun getGroupId(projectId_: String) = dbQuery {
        ProjectsCuratorship.selectAll()
            .where { ProjectsCuratorship.projectId eq projectId_.toInt()}
            .map {
                it[ProjectsCuratorship.projectGroupId]
            }
            .first()
    }

    suspend fun getProjectGroupIds(groupId_: Int) = dbQuery {
        ProjectsCuratorship.selectAll()
            .where { ProjectsCuratorship.projectGroupId eq groupId_ }
            .map {
                it[ProjectsCuratorship.projectId]
            }
    }

    suspend fun getStatus(projectId_: Int) = dbQuery {
        ProjectsCuratorship.selectAll()
            .where { ProjectsCuratorship.projectId eq projectId_ }
            .map {
                it[ProjectsCuratorship.status]
            }
            .first()
    }

    suspend fun getPendingProjectIds(curatorId: Int) = dbQuery {
        ProjectsCuratorship.selectAll()
            .where {
                (ProjectsCuratorship.userId eq curatorId) and
                        (ProjectsCuratorship.status eq ProjectStatus.Pending)
            }
            .map {
                it[ProjectsCuratorship.projectId]
            }
    }

    suspend fun getUnmarkedProjectIds(curatorId: Int) = dbQuery {
        ProjectsCuratorship.selectAll()
            .where {
                (ProjectsCuratorship.userId eq curatorId) and
                        (ProjectsCuratorship.mark eq null) and
                        (ProjectsCuratorship.status eq ProjectStatus.Approved)
            }
            .map {
                it[ProjectsCuratorship.projectId]
            }
    }

    suspend fun getMark(projectId_: Int)  = dbQuery {
        ProjectsCuratorship.selectAll()
            .where { ProjectsCuratorship.projectId eq projectId_ }
            .map {
                it[ProjectsCuratorship.mark]
            }
            .first()
    }

    suspend fun getStatusToMark(projectId_: Int)  = dbQuery {
        ProjectsCuratorship.selectAll()
            .where { ProjectsCuratorship.projectId eq projectId_ }
            .map {
                it[ProjectsCuratorship.status] to it[ProjectsCuratorship.mark]
            }
            .first()
    }

    suspend fun setMark(projectId_: Int, mark_: Int) = dbQuery {
        ProjectsCuratorship.update(
            where = { ProjectsCuratorship.projectId eq projectId_.toInt() }
        ) {
            it[mark] = mark_
        }
    }
}