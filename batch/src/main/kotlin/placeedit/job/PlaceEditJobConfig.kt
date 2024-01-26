package com.wafflestudio.snu4t.placeedit.job

import com.wafflestudio.snu4t.common.extension.isEqualTo
import com.wafflestudio.snu4t.lectures.data.Lecture
import com.wafflestudio.snu4t.lectures.repository.LectureRepository
import com.wafflestudio.snu4t.timetables.data.Timetable
import com.wafflestudio.snu4t.timetables.data.TimetableLecture
import com.wafflestudio.snu4t.timetables.repository.TimetableRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.transaction.PlatformTransactionManager
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Configuration
class PlaceEditJobConfig(
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val timetableRepository: TimetableRepository,
    private val lectureRepository: LectureRepository,
) {

    @Bean
    fun placeEditJob(jobRepository: JobRepository, placeEditStep: Step): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .start(placeEditStep)
            .build()
    }

    @Bean
    @JobScope
    fun placeEditStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager
//        @Value("#{jobParameters[year]}") year: Int
    ): Step = StepBuilder(STEP_NAME, jobRepository).tasklet(
        { _, _ ->
            val result = checkPlaceEdits(2023)
            log.info("## Total ${result.totalCount}, Update ${result.updateCount}, Distinct User ${result.distinctUserCount}")

            RepeatStatus.FINISHED
        },
        transactionManager
    ).build()

    class Result(val distinctUserCount: Int, val updateCount: Int, val totalCount: Int)
    private fun checkPlaceEdits(year: Int): Result = runBlocking {
        val atomicUserSet = AtomicReference(mutableSetOf<String>())
        var totalCount = AtomicInteger()
        var updateCount = AtomicInteger()

        val channel = Channel<Timetable>(capacity = 100)

        launch {
            for (table in channel) {
                findTimeTableWithPlaceEdit(table, totalCount, updateCount, atomicUserSet)
                totalCount.getAndIncrement()
            }
        }

        reactiveMongoTemplate.find<Timetable>(Query.query(Timetable::year isEqualTo 2023))
            .asFlow()
            .collect { channel.send(it) }

        channel.close()
        return@runBlocking Result(atomicUserSet.get().count(), updateCount.get(), totalCount.get())
    }

    private suspend fun findTimeTableWithPlaceEdit(
        table: Timetable,
        totalCount: AtomicInteger,
        updateCount: AtomicInteger,
        userSet: AtomicReference<MutableSet<String>>
    ) {
        val timeTableLectures = table.lectures.sortedBy { it.lectureId }
        val lectures = lectureRepository.findAllById(timeTableLectures.map { it.lectureId }.filterNotNull())
            .toList()
            .sortedBy { it.id }

        val placeUpdated = timeTableLectures
            .map { timetableLecture ->
                val lecture = lectures.firstOrNull { it.id == timetableLecture.lectureId }
                Pair<TimetableLecture, Lecture?>(timetableLecture, lecture)
            }
            .filter { it.second != null }
            .filter {
                it.first.classPlaceAndTimes.sortedBy { it.day }.map { it.place } != it.second?.classPlaceAndTimes?.sortedBy { it.day }?.map { it.place }
            }
            .map {
                "${
                it.second?.classPlaceAndTimes?.map { it.place }?.joinToString(", ")
                } -> ${
                it.first.classPlaceAndTimes.map { it.place }.joinToString(", ")
                }"
            }

        if (placeUpdated.isNotEmpty()) {
            updateCount.incrementAndGet()
            userSet.getAndUpdate {
                it.add(table.userId)
                return@getAndUpdate it
            }
            log.info("# Update $updateCount/$totalCount: ${placeUpdated.joinToString(" | ")}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
        private const val BULK_WRITE_SIZE = 100
        const val JOB_NAME = "placeEditJob"
        const val STEP_NAME = "placeEditStep"
    }
}
