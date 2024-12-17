package domain

interface InvitesRepo {

    suspend fun getProjectInvite(projectId: String): String

    suspend fun refreshProjectInvite(projectId: String): String

    suspend fun useInvite(invite: String, userId: String): String?
}