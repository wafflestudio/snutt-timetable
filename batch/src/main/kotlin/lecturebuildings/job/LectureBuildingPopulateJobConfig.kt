package com.wafflestudio.snu4t.lecturebuildings.job

import com.wafflestudio.snu4t.common.enum.Semester
import com.wafflestudio.snu4t.lecturebuildings.data.LectureBuildingUpdateResult
import com.wafflestudio.snu4t.lecturebuildings.service.LectureBuildingPopulateService
import com.wafflestudio.snu4t.lectures.data.Lecture
import com.wafflestudio.snu4t.lectures.repository.LectureRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class LectureBuildingPopulateJobConfig(
    private val lectureRepository: LectureRepository,
    private val lectureBuildingPopulateService: LectureBuildingPopulateService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val JOB_NAME = "lectureBuildingPopulateJob"
        const val STEP_NAME = "lectureBuildingPopulateStep"
    }

    @Bean
    fun lectureBuildingPopulateJob(jobRepository: JobRepository, lectureBuildingPopulateStep: Step): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .start(lectureBuildingPopulateStep)
            .build()
    }

    @Bean
    @JobScope
    fun lectureBuildingPopulateStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        @Value("#{jobParameters[year]}") year: Int,
        @Value("#{jobParameters[semester]}") semester: Int,
    ): Step = StepBuilder(STEP_NAME, jobRepository).tasklet(
        { _, _ ->
            runBlocking {
                val lectures = lectureRepository.findAllByYearAndSemester(year, Semester.getOfValue(semester)!!).toList()
                lectureBuildingPopulateService.fetchMissingBuildingInfo(lectures)
                val updateResult = updateBuildingInfoOfLectures(lectures)
                updateTimetableLectures(updateResult.lecturesWithBuildingInfos)
            }
            RepeatStatus.FINISHED
        },
        transactionManager
    ).build()

    private suspend fun updateBuildingInfoOfLectures(lectures: List<Lecture>): LectureBuildingUpdateResult =
        lectureBuildingPopulateService.updateBuildingInfoOfLectures(lectures).also { logUpdateResult(it) }

    private suspend fun updateTimetableLectures(lectures: List<Lecture>) = coroutineScope {
        lectures.map {
            async {
                val timetables = lectureBuildingPopulateService.populateLectureBuildingsOfTimetables(it)
                log.info("강의 ${it.courseTitle}(${it.courseNumber})}가 포함된 시간표 ${timetables.count()}개를 업데이트 했습니다.")
            }
        }.awaitAll()
    }

    private fun logUpdateResult(updateResult: LectureBuildingUpdateResult) {
        log.info(
            """
                강의 ${updateResult.lecturesWithBuildingInfos.count()}개의 강의동 정보를 업데이트 했습니다.
                ${updateResult.lecturesWithBuildingInfos.joinToString(", ") { "${it.courseTitle}(${it.lectureNumber})" }}
            """.trimIndent()
        )
        log.info(
            """
                강의동 업데이트에 실패한 강의:
                ${
                updateResult.lecturesFailed.joinToString("\n") { lecture ->
                    "${lecture.courseTitle}(${lecture.lectureNumber}): ${lecture.classPlaceAndTimes.joinToString(", ") { it.place }}"
                }
            }
            """.trimIndent()
        )
    }
}
