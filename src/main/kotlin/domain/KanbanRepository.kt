package domain

import shared_domain.entities.KanbanState

interface KanbanRepository {

    suspend fun getKanban(projectId: Int, onlyKardIds: List<String>? = null): KanbanState

    suspend fun createKard(name: String, desc: String, columnId: Int, authorId: String)

    suspend fun moveKard(columnId: Int, kardId: Int, newColumnId: Int, newOrder: Int)

    suspend fun createColumn(projectId: Int, name: String)

    suspend fun moveColumn(projectId: Int, columnId: Int, newOrder: Int)

    suspend fun updateKard(id: Int, name: String?, desc: String?)

    suspend fun updateColumn(id: Int, name: String?)

    suspend fun deleteKard(id: Int)

    suspend fun deleteColumn(id: Int)

    suspend fun getKardTitle(id: Int): String

    suspend fun getColumnTitle(id: Int): String
}