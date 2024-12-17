package app.features.projectgroups

import domain.projectgroups.ProjectInGroupView
import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectGroupRequest(
    val title: String
)

@Serializable
data class GroupProjectsResponse(
    val title: String,
    val projects: List<ProjectInGroupView>
)