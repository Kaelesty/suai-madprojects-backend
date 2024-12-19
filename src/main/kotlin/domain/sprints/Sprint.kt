package domain.sprints

import kotlinx.serialization.Serializable
import shared_domain.entities.KanbanState

@Serializable
data class ProfileSprint(
    val id: String,
    val startDate: String,
    val actualEndDate: String?,
    val supposedEndDate: String,
    val title: String,
)

@Serializable
data class Sprint(
    val meta: SprintMeta,
    val kardIds: List<String>,
)

@Serializable
data class SprintView(
    val meta: SprintMeta,
    val kanban: KanbanState
)

@Serializable
data class SprintMeta(
    val id: String,
    val startDate: String,
    val actualEndDate: String?,
    val supposedEndDate: String,
    val title: String,
    val desc: String,
)