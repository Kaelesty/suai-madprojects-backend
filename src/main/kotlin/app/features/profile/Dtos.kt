package app.features.profile

import domain.auth.UserType
import domain.projectgroups.ProjectGroup
import kotlinx.serialization.Serializable
import shared_domain.entities.GithubUserMeta

@Serializable
data class UpdateProfileRequest(
    val firstName: String?,
    val secondName: String?,
    val lastName: String?,
    val data: String?,
)

@Serializable
data class SharedProfileResponse(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val avatar: String?,
    val data: String,
    val githubLink: String?,
    val email: String,
    val role: UserType,
)

@Serializable
data class CuratorProfileResponse(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val email: String,
    val grade: String,
    val githubMeta: GithubUserMeta?,
    val projectGroups: List<ProjectGroup>
)