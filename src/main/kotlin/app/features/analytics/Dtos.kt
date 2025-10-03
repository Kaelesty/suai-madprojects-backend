package app.features.analytics

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