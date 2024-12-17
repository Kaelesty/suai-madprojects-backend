package domain.activity

import kotlinx.serialization.Serializable

@Serializable
data class Activity(
    val type: ActivityType,
    val timeMillis: Long,
    val actorId: String?,
    val targetTitle: String,
    val targetId: String?,
    val secondaryTargetTitle: String?
)

@Serializable
enum class ActivityType {
    RepoBind, RepoUnbind,
    SprintStart, SprintFinish,
    KardMove,
    MemberAdd, MemberRemove,
}