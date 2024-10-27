package shared_domain.entities

import kotlinx.serialization.Serializable

@Serializable
class KanbanState(
    val columns: List<Column>
) {

    @Serializable
    data class Column(
        val id: Int,
        val name: String,
        val kards: List<Kard>
    )

    @Serializable
    data class Kard(
        val id: Int,
        val authorName: String,
        val createdTimeMillis: Long,
        val updateTimeMillis: Long,
        val title: String,
        val desc: String,
    )
}