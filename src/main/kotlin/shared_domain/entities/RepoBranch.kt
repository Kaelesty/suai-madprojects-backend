package shared_domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class RepoBranch(
    val name: String,
    val commits: List<Commit>,
    val creators: List<Member>,
)

@Serializable
data class Commit(
    val id: String,
    val creatorId: String,
    val createdMillis: Long,
    val title: String,
    val link: String,
)

@Serializable
data class Member(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val secondName: String
)