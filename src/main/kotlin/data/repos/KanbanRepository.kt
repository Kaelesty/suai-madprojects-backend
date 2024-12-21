package data.repos

import data.schemas.ColumnsService
import data.schemas.KardInSprintService
import data.schemas.KardOrdersService
import data.schemas.KardService
import data.schemas.UserService
import domain.KanbanRepository
import domain.auth.User
import shared_domain.entities.KanbanState

class KanbanRepositoryImpl(
    private val kardService: KardService,
    private val columnsService: ColumnsService,
    private val kardOrdersService: KardOrdersService,
    private val kardInSprintService: KardInSprintService,
    private val userService: UserService,
) : KanbanRepository {

    override suspend fun getKardTitle(id: Int): String {
        return kardService.getById(id).name
    }

    override suspend fun getColumnTitle(id: Int): String {
        return columnsService.getById(id).title
    }

    override suspend fun deleteKard(id: Int) {
        val changedColumns = kardOrdersService.getKardColumns(id)
        kardOrdersService.delete(id)
        changedColumns.forEach {
            kardOrdersService.recalculateOrders(it)
        }
        kardService.deleteKard(id)
        kardInSprintService.onKardDeletion(id.toString())
    }

    override suspend fun deleteColumn(id: Int) {
        val projectId = columnsService.getProjectId(id)
        kardOrdersService.getColumnKards(id).forEach {
            deleteKard(it.kardId)
        }
        columnsService.deleteColumn(id)
        columnsService.recalculateOrders(projectId)
    }

    override suspend fun getKanban(projectId: Int, onlyKardIds: List<String>?): KanbanState {

        val columns = columnsService.getProjectColumns(projectId)
        val users = mutableMapOf<Int, User?>()

        return KanbanState(
            columns = columns
                .sortedBy { it.order }
                .map {

                    val kardOrders = kardOrdersService.getColumnKards(it.id)

                    val column = KanbanState.Column(
                        id = it.id,
                        name = it.title,
                        kards = kardOrders
                            .sortedBy { it.order }
                            .filter {
                                if (onlyKardIds == null) true else {
                                    onlyKardIds.contains(it.kardId.toString())
                                }
                            }
                            .map {

                                val kard = kardService.getById(it.kardId)
                                val creatorId = kard.authorName.toInt()
                                val user = if (users.containsKey(creatorId)) {
                                    users[creatorId]
                                } else {
                                    userService.getById(creatorId).also {
                                        users[creatorId] = it
                                    }
                                }

                                KanbanState.Kard(
                                    id = it.kardId,
                                    authorName = if (user == null) "..." else "${user.lastName} ${user.firstName}",
                                    createdTimeMillis = kard.createTimeMillis,
                                    updateTimeMillis = kard.updateTimeMillis,
                                    title = kard.name,
                                    desc = kard.desc
                                )
                            }
                    )

                    if (column.kards.isEmpty() && onlyKardIds != null) null else column
                }.filterNotNull()
        )
    }

    override suspend fun createKard(
        name: String,
        desc: String,
        columnId: Int,
        authorId: String
    ) {
        val newId = kardService.create(name, desc, authorId)
        kardOrdersService.create(
            kardId_ = newId,
            columnId_ = columnId
        )
    }

    override suspend fun moveKard(
        columnId: Int,
        kardId: Int,
        newColumnId: Int,
        newOrder: Int
    ) {

        if (columnId == newColumnId) {
            // just update orders
            val kardOrders = kardOrdersService.getColumnKards(columnId)
            var currentOrder = -1
            val orders = kardOrders
                .sortedBy { it.order }
                .mapIndexed { index, it ->
                    if (it.kardId == kardId) currentOrder = index
                    it.kardId
                }
                .toMutableList()
            if (currentOrder == -1) throw IllegalStateException("No Kard found with id $kardId")
            if (newOrder > currentOrder) {
                orders.removeAt(currentOrder)
                orders.add(newOrder, kardId)
            } else {
                orders.add(newOrder, kardId)
                orders.removeAt(currentOrder + 1)
            }
            kardOrdersService.updateKardOrders(
                columnId_ = columnId,
                orders = orders.mapIndexed { index, it ->
                    KardOrdersService.NewKardOrder(
                        kardId = it,
                        order = index
                    )
                }
            )
        } else {
            // del from old column & insert into new
            kardOrdersService.delFromColumnByKardId(
                columnId_ = columnId,
                kardId_ = kardId
            )
            kardOrdersService.recalculateOrders(columnId)
            val orders = kardOrdersService.getColumnKards(newColumnId)
                .sortedBy { it.order }
                .map { it.kardId }
                .toMutableList()
            orders.add(newOrder, kardId)
            kardOrdersService.create(kardId, newColumnId)
            kardOrdersService.updateKardOrders(
                columnId_ = newColumnId,
                orders = orders.mapIndexed { index, it ->
                    KardOrdersService.NewKardOrder(
                        kardId = it,
                        order = index
                    )
                }
            )
        }
        kardService.updateKardTime(kardId)
    }

    override suspend fun createColumn(projectId: Int, name: String) {
        columnsService.create(projectId, name)
    }

    override suspend fun moveColumn(projectId: Int, rowId: Int, newOrder: Int) {
        var currentOrder = -1
        val orders = columnsService.getProjectColumns(projectId_ = projectId)
            .sortedBy { it.order }
            .mapIndexed { index, it ->
                if (it.id == rowId) currentOrder = index
                it.id
            }
            .toMutableList()
        if (currentOrder == -1) throw IllegalStateException("No Row found with id $rowId")
        if (newOrder > currentOrder) {
            orders.removeAt(currentOrder)
            orders.add(newOrder, rowId)
        } else {
            orders.add(newOrder, rowId)
            orders.removeAt(currentOrder + 1)
        }
        columnsService.updateColumnsOrders(
            projectId_ = projectId,
            newOrders = orders.mapIndexed { index, it ->
                ColumnsService.NewColumnOrder(
                    rowId = it,
                    order = index
                )
            }
        )
    }

    override suspend fun updateKard(id: Int, name: String?, desc: String?) {
        kardService.update(
            id, name, desc
        )
    }

    override suspend fun updateColumn(id: Int, name: String?) {
        columnsService.update(id, name)
    }
}