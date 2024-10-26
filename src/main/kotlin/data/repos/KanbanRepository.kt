package data.repos

import data.schemas.ColumnsService
import data.schemas.KardOrdersService
import data.schemas.KardService
import domain.KanbanRepository
import shared_domain.entities.KanbanState

class KanbanRepository(
    private val kardService: KardService,
    private val columnsService: ColumnsService,
    private val kardOrdersService: KardOrdersService,
): KanbanRepository {

    override suspend fun getKanban(projectId: Int): KanbanState {
        TODO("Not yet implemented")
    }

    override suspend fun createKard(name: String, desc: String, columnId: Int) {
        val newId = kardService.create(name, desc)
        kardOrdersService.create(
            kardId_ = newId,
            columnId_ = columnId
        )
    }

    override suspend fun moveKard(
        rowId: Int,
        kardId: Int,
        newColumnId: Int,
        newOrder: Int
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun createColumn(projectId: Int, name: String) {
        columnsService.create(projectId, name)
    }

    override suspend fun moveColumn(projectId: Int, rowId: Int, newOrder: Int) {
        TODO("Not yet implemented")
    }
}