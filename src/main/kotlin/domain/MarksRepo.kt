package domain

interface MarksRepo {

    suspend fun markProject(projectId: String, mark: Int)

    suspend fun getProjectMark(projectId: String): Int?
}