package app

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.Date

const val USERS_COUNT = 500
const val REQUESTS_COUNT = 5
const val REQUEST_DELAY_MILLIS = 100L

class Uzver(
    private val httpClient: HttpClient,
    private val token: String,
    private val id: Int,
    private val onMeasure: (Long) -> Unit,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(): Job {
        return scope.launch {
            repeat(REQUESTS_COUNT) {
                delay(REQUEST_DELAY_MILLIS)
                println("Uzder[$id] Request[$it]")
                val time = System.currentTimeMillis()
                httpClient.get("https://mad-projects.ru:8080/commonProfile") {
                    header("Authorization", "Bearer $token")
                }
                onMeasure(
                    System.currentTimeMillis() - time
                )
            }
        }
    }
}

suspend fun main() {

    val lock = MutableStateFlow(true)
    val scope = CoroutineScope(SupervisorJob())

    scope.launch {
        val accessToken = JWT.create()
            .withClaim("userId", 1)
            .withExpiresAt(Date(System.currentTimeMillis() + 1000L * 60 * 60))
            .sign(Algorithm.HMAC256("32WARF*_+@!t3wesdE_D"))
        val httpClient = HttpClient()
        val token = accessToken
        val uzvers = mutableListOf<Uzver>()
        val measures = mutableListOf<Long>()
        repeat(USERS_COUNT) {
            uzvers.add(
                Uzver(httpClient, token, it) {
                    measures.add(it)
                }
            )
        }
        val jobs = mutableListOf<Job>()
        uzvers.forEach {
            jobs.add(it.start())
        }

        jobs.forEach { it.join() }

        println("Users count:\t${USERS_COUNT}")
        println("Requests count:\t${REQUESTS_COUNT}")
        println("Request delay:\t${REQUEST_DELAY_MILLIS}ms")
        println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
        println("Average request time:\t${measures.average() / 1000}")
        lock.emit(false)
    }

    scope.launch {
        lock.collect {
            if (!it) return@collect
        }
    }.join()
}
