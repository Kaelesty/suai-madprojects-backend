package domain.projectgroups

interface ProjectsGroupRepo {

    suspend fun createProjectsGroup(title: String, curatorId: String): ProjectGroup

    suspend fun getCuratorProjectGroups(curatorId: String): List<ProjectGroup>

    suspend fun getGroupProjects(groupId: String): List<ProjectInGroupView>

    suspend fun checkIsCuratorGroupOwner(curatorId: String, groupId: String): Boolean

    suspend fun getGroupTitle(groupId: String): String
}