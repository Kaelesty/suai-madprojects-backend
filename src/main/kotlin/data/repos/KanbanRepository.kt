package data.repos

import data.schemas.ColumnsService
import data.schemas.KardOrdersService
import data.schemas.KardService
import domain.KanbanRepository
import shared_domain.entities.KanbanState

class KanbanRepositoryImpl(
    private val kardService: KardService,
    private val columnsService: ColumnsService,
    private val kardOrdersService: KardOrdersService,
): KanbanRepository {

    override suspend fun getKanban(projectId: Int): KanbanState {

        val columns = columnsService.getProjectColumns(projectId)

        return KanbanState(
            columns = columns.map {

                val kardOrders = kardOrdersService.getColumnKards(it.id)

                KanbanState.Column(
                    id = it.id,
                    name = it.title,
                    kards = kardOrders.map {

                        val kard = kardService.getById(it.id)

                        KanbanState.Kard(
                            id = it.id,
                            authorName = kard.authorName,
                            createdTimeMillis = kard.createTimeMillis,
                            updateTimeMillis = kard.updateTimeMillis,
                            title = kard.name,
                            desc = kard.desc
                        )
                    }
                )
            }
        )
    }

    override suspend fun createKard(
        name: String,
        desc: String,
        columnId: Int,
        authorId: Int
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
                orders.add(newOrder, kardId)
                orders.removeAt(currentOrder)
            }
            else {
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
        }

        else {
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
            orders.add(newOrder, rowId)
            orders.removeAt(currentOrder)
        }
        else {
            orders.add(newOrder, rowId)
            orders.removeAt(currentOrder + 1)
        }
    }
}