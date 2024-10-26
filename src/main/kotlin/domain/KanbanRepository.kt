package domain

import shared_domain.entities.KanbanState

interface KanbanRepository {

    suspend fun getKanban(projectId: Int): KanbanState

    suspend fun createKard(name: String, desc: String, columnId: Int)

    suspend fun moveKard(rowId: Int, kardId: Int, newColumnId: Int, newOrder: Int)

    suspend fun createColumn(projectId: Int, name: String)

    suspend fun moveColumn(projectId: Int, rowId: Int, newOrder: Int)
}