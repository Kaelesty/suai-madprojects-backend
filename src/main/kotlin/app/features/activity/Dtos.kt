package app.features.activity

import domain.activity.Activity
import domain.profile.SharedProfile
import kotlinx.serialization.Serializable

@Serializable
data class ActivityResponse(
    val activities: List<Activity>,
    val actors: Map<String, SharedProfile>,
)