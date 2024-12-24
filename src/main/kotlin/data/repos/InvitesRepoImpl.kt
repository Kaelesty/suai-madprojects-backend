package data.repos

import data.schemas.InvitesService
import data.schemas.ProjectMembershipService
import data.schemas.ProjectService
import domain.InvitesRepo
import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID

class InvitesRepoImpl(
    private val invitesService: InvitesService,
    private val projectMembershipService: ProjectMembershipService,
    private val projectService: ProjectService,
) : InvitesRepo {

    override suspend fun getProjectInvite(projectId: String): String {
        val invite = invitesService.getByProjectId(projectId.toInt())
        if (invite == null) {
            val newInvite = generateInvite(projectId)
            invitesService.create(projectId.toInt(), newInvite)
            return newInvite
        }
        return invite
    }

    override suspend fun refreshProjectInvite(projectId: String): String {
        var invite = ""
        do {
            invite = generateInvite(projectId)
        } while (!invitesService.checkIsUnique(invite))
        invitesService.refreshByProjectId(projectId.toInt(), invite)
        return invite
    }

    override suspend fun useInvite(invite: String, userId: String): String? {
        val projectId = invitesService.getProjectByInvite(invite)
        projectId?.let {
            val project = projectService.getById(projectId)
            val projectMembersCount = projectMembershipService.getProjectUserIds(projectId.toString()).size
            if (project.maxMembersCount == projectMembersCount) {
                return null
            }
            projectMembershipService.create(it.toString(), userId)
        }
        return projectId?.toString()
    }

    private fun generateInvite(projectId: String): String {
        val md = MessageDigest.getInstance("MD5")
        val uuid = UUID.randomUUID()
        return BigInteger(
            1,
            md.digest(
                "pr_${projectId}_inv_${uuid}".toByteArray()
            )
        ).toString(16).padStart(
            32, '0'
        )
    }
}