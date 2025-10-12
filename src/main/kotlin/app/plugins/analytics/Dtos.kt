package app.plugins.analytics

import kotlinx.serialization.Serializable

@Serializable
data class ProjectCommits(
    val projectId: String,
    val count: Int,
    val projectName: String,
)

@Serializable
data class MemberWithMark(
    val firstName: String,
    val lastName: String,
    val secondName: String,
    val mark: Int?,
    val group: String,
)

@Serializable
data class ProjectWithStatus(
    val id: String,
    val title: String,
    val status: String,
)

@Serializable
data class ProjectWithCommitsCount(
    val id: String,
    val title: String,
    val count: Int,
)

@Serializable
data class ProjectTitleToId(
    val title: String,
    val statusName: String,
)

@Serializable
data class ProjectWithMark(
    val id: String,
    val title: String,
    val mark: Int?
)