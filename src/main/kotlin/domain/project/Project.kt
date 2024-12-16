package domain.project

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val meta: ProjectMeta,
    val members: List<ProjectMember>,
    val repos: List<ProjectRepository>,
    val isCreator: Boolean,
)

@Serializable
data class ProjectMeta(
    val id: String,
    val title: String,
    val desc: String,
    val maxMembersCount: Int,
    val createDate: String
)

@Serializable
data class ProjectMember(
    val id: String,
    val firstName: String,
    val lastName: String,
    val secondName: String,
)

@Serializable
data class ProjectRepository(
    val id: String,
    val link: String,
    val title: String,
)