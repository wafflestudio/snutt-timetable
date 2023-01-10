package com.wafflestudio.snu4t

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.internal.MongoClientImpl
import com.wafflestudio.snu4t.timetables.data.TimeTable
import com.wafflestudio.snu4t.timetables.repository.TimeTableRepository
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringTestExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.event.EventListener
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.test.context.event.BeforeTestExecutionEvent
import java.time.Instant

@SpringBootApplication
class TestApplication(
    private val mongoClient: MongoClient,
    private val repositories: List<CoroutineCrudRepository<*, *>>,
) {

    class ProjectConfig : AbstractProjectConfig() {
        override fun extensions() = listOf(SpringTestExtension(SpringTestLifecycleMode.Root))
        override val isolationMode: IsolationMode = IsolationMode.InstancePerLeaf
    }

    @EventListener(BeforeTestExecutionEvent::class)
    fun truncateCollections(): Unit = runBlocking {
        val hosts = (mongoClient as MongoClientImpl).settings.clusterSettings.hosts

        // embedded인 경우에만 truncate
        require(hosts.none { !it.host.startsWith("localhost") })

        repositories.forEach { it.deleteAll() }
    }
}

@SpringBootTest
class TruncateCollectionsTest(
    private val repository: TimeTableRepository,
) : BehaviorSpec() {

    init {
        Given("a dummy data") {
            val dummy = TimeTable(
                userId = "63a1b97f42ed0d0010b4351d",
                year = 2022,
                semester = 1,
                lectures = listOf(),
                title = "test",
                theme = 0,
                updatedAt = Instant.now()
            )

            When("I save it.") {
                repository.save(dummy)

                Then("I got single data.") {
                    val timeTables = repository.findAllByUserId(dummy.userId)
                        .toList()

                    timeTables.size shouldBe 1
                }

                Then("I still got single data.") {
                    val timeTables = repository.findAllByUserId(dummy.userId)
                        .toList()

                    timeTables.size shouldBe 1
                }
            }
        }
    }
}
