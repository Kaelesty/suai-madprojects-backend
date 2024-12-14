package domain.project

interface ProjectRepo {

    suspend fun getCuratorsList(): List<AvailableCurator>
}