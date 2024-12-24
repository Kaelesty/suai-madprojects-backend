package domain.projectgroups

import domain.project.ProjectStatus
import kotlinx.serialization.Serializable

@Serializable
data class ProjectGroup(
    val id: String,
    val curatorId: String,
    val title: String
)

@Serializable
data class ProjectInGroupView(
    val id: String,
    val title: String,
    val members: List<ProjectInGroupMember>,
    val createDate: String,
    val status: ProjectStatus,
    val maxMembersCount: Int,
    val groupTitle: String,
)

@Serializable
data class ProjectInGroupMember(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val group: String,
)
