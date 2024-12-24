package data.repos.branches

import app.features.github.Commiter
import data.schemas.GithubService
import data.schemas.ProjectReposService
import data.schemas.UserService
import domain.BranchesRepo
import domain.CommiterModel
import domain.profile.SharedProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import shared_domain.entities.Branch
import shared_domain.entities.BranchCommit
import shared_domain.entities.BranchCommitView
import shared_domain.entities.BranchCommits
import shared_domain.entities.RepoBranchView
import shared_domain.entities.RepoView

class BranchesRepoImpl(
    private val httpClient: HttpClient,
    private val reposService: ProjectReposService,
    private val githubService: GithubService,
    private val userService: UserService,
) : BranchesRepo {

    private val COMMITS_PAGE_SIZE = 100
    private val MAX_COMMITS = 500

    private val githubRepoLink = "https://api.github.com/repos"

    private val cachedProjectRepoBranchLists = mutableMapOf<String, List<RepoView>>()
    private val cachedRepoBranches = mutableMapOf<String, Pair<String, BranchCommits>>()

    override suspend fun getCommitsCount(projectId: String, githubJwt: String): List<CommiterModel> {

        if (!cachedProjectRepoBranchLists.keys.contains(projectId)) {
            getProjectRepoBranches(projectId, githubJwt)
        }

        val separated = cachedProjectRepoBranchLists[projectId]!!.map { repo ->
            repo.repoBranches.first().let {
                if (!cachedRepoBranches.keys.contains(it.sha)) {
                    getRepoBranchContent(
                        sha = it.sha,
                        repoName = repo.name,
                        githubJwt = githubJwt,
                        profileMaker = {
                            githubService.getUserId(it)?.let {
                                userService.getById(it.toInt())?.let {
                                    SharedProfile(
                                        firstName = it.firstName,
                                        secondName = it.secondName,
                                        lastName = it.lastName
                                    )
                                }
                            }
                        }
                    )
                }
                cachedRepoBranches[it.sha]!!.let { pair ->
                    pair.second.authors.map { commiter ->
                        commiter.profile?.let {
                            CommiterModel(
                                fullName = "${it.lastName} ${it.firstName}",
                                commitsCount = pair.second.commits
                                    .filter { it.authorGithubId == commiter.githubMeta?.githubId?.toInt() }
                                    .size
                            )
                        }
                    }.filterNotNull()
                }
            }
        }

        if (separated.isEmpty()) {
            return listOf()
        }

        var united = separated.first().map { it }.toMutableList()
        val authors = united.map { it.fullName }.toMutableList()
        separated.forEach {
            it.forEach {  commiter ->
                if (authors.contains(commiter.fullName)) {
                    united = united.map { if (it.fullName == commiter.fullName) it.copy(commitsCount = it.commitsCount + commiter.commitsCount) else it }.toMutableList()
                }
                else {
                    united.add(commiter)
                    authors.add(commiter.fullName)
                }
            }
        }
        return united
    }

    override suspend fun getProjectRepoBranches(
        projectId: String,
        githubJwt: String,
    ): List<RepoView>? {

        val projectRepos = reposService.getByProjectId(projectId.toInt())
        val repos = mutableListOf<RepoView>()

        println("Getting {$projectId repo branches}")

        projectRepos.forEach {
            println("Getting {${it.second}}")
            val repoBranches = mutableListOf<RepoBranchView>()
            val parts = it.second.split("/").reversed()
            val response = httpClient.get("$githubRepoLink/${parts[1]}/${parts[0]}/branches") {
                header("Authorization", "Bearer $githubJwt")
            }
            if (response.status == HttpStatusCode.OK) {
                try {
                    val body = response.body<List<Branch>>()
                    body.forEach { branch ->
                        repoBranches.add(
                            RepoBranchView(
                                name = "${parts[0]}/${branch.name}",
                                sha = branch.data.sha,
                            )
                        )
                    }

                    repos.add(
                        RepoView(
                            name = "${parts[1]}/${parts[0]}",
                            repoBranches
                        )
                    )
                } catch (_: Exception) {
                    return null
                }
            } else {
                return null
            }
        }
        return repos.toList().also {
            cachedProjectRepoBranchLists[projectId] = it
        }
    }

    override suspend fun getRepoBranchContent(
        sha: String,
        repoName: String,
        githubJwt: String,
        profileMaker: suspend (Int) -> SharedProfile?,
    ): BranchCommits? {
        var pageCounter = 0
        val commits = mutableListOf<BranchCommitView>()

        while (true) {
            val response = httpClient.get(
                "$githubRepoLink/$repoName/commits?per_page=$COMMITS_PAGE_SIZE&sha=$sha&page=$pageCounter"
            ) {
                header("Authorization", "Bearer $githubJwt")
            }
            if (response.status == HttpStatusCode.OK) {
                try {
                    val body = response.body<List<BranchCommit>>()

                    if (body.isEmpty()) {
                        break
                    }

                    body
                        .map {
                            BranchCommitView(
                                sha = it.sha,
                                authorGithubId = it.author?.id ?: -1,
                                date = it.data.author.date,
                                message = it.data.message
                            )
                        }
                        .forEach {
                            commits.add(it)
                        }

                    if (body.size < COMMITS_PAGE_SIZE || commits.size > MAX_COMMITS) {
                        break
                    }
                    pageCounter += 1

                } catch (e: Exception) {
                    e.toString()
                    response.status
                    return null
                }
            } else {
                return null
            }
        }

        val authorIds = commits.map { it.authorGithubId }
        val authors = authorIds.distinct().map { githubUserId ->
            Commiter(
                githubMeta = githubService.getUserMeta(githubUserId),
                profile = profileMaker(githubUserId)
            )
        }

        return BranchCommits(
            commits = commits,
            authors = authors
        ).also {
            cachedRepoBranches[sha] = repoName to it
        }
    }
}