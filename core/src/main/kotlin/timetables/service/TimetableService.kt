package com.wafflestudio.snutt.timetables.service

import com.wafflestudio.snutt.common.dynamiclink.client.DynamicLinkClient
import com.wafflestudio.snutt.common.dynamiclink.dto.DynamicLinkResponse
import com.wafflestudio.snutt.common.enum.BasicThemeType
import com.wafflestudio.snutt.common.enum.Semester
import com.wafflestudio.snutt.common.exception.DuplicateTimetableTitleException
import com.wafflestudio.snutt.common.exception.InvalidTimetableTitleException
import com.wafflestudio.snutt.common.exception.PrimaryTimetableNotFoundException
import com.wafflestudio.snutt.common.exception.TableDeleteErrorException
import com.wafflestudio.snutt.common.exception.TimetableNotFoundException
import com.wafflestudio.snutt.common.exception.TimetableNotPrimaryException
import com.wafflestudio.snutt.coursebook.data.CoursebookDto
import com.wafflestudio.snutt.coursebook.service.CoursebookService
import com.wafflestudio.snutt.evaluation.repository.SnuttEvRepository
import com.wafflestudio.snutt.theme.service.TimetableThemeService
import com.wafflestudio.snutt.theme.service.toBasicThemeType
import com.wafflestudio.snutt.theme.service.toIdForTimetable
import com.wafflestudio.snutt.timetables.data.Timetable
import com.wafflestudio.snutt.timetables.dto.TimetableDto
import com.wafflestudio.snutt.timetables.dto.TimetableLectureDto
import com.wafflestudio.snutt.timetables.dto.TimetableLectureLegacyDto
import com.wafflestudio.snutt.timetables.dto.TimetableLegacyDto
import com.wafflestudio.snutt.timetables.dto.request.TimetableAddRequestDto
import com.wafflestudio.snutt.timetables.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

interface TimetableService {
    suspend fun getTimetables(userId: String): List<Timetable>

    suspend fun getMostRecentlyUpdatedTimetable(userId: String): Timetable

    fun getTimetablesBySemester(
        userId: String,
        year: Int,
        semester: Semester,
    ): Flow<Timetable>

    suspend fun addTimetable(
        userId: String,
        timetableRequest: TimetableAddRequestDto,
    ): Timetable

    suspend fun getTimetableLink(
        userId: String,
        timetableId: String,
    ): DynamicLinkResponse

    suspend fun getTimetable(
        userId: String,
        timetableId: String,
    ): Timetable

    suspend fun modifyTimetableTitle(
        userId: String,
        timetableId: String,
        title: String,
    ): Timetable

    suspend fun deleteTimetable(
        userId: String,
        timetableId: String,
    )

    suspend fun modifyTimetableTheme(
        userId: String,
        timetableId: String,
        basicThemeType: BasicThemeType?,
        themeId: String?,
    ): Timetable

    suspend fun copyTimetable(
        userId: String,
        timetableId: String,
        title: String? = null,
    ): Timetable

    suspend fun getUserPrimaryTable(
        userId: String,
        year: Int,
        semester: Semester,
    ): Timetable

    suspend fun getCoursebooksWithPrimaryTable(userId: String): List<CoursebookDto>

    suspend fun createDefaultTable(userId: String): Timetable

    suspend fun setPrimary(
        userId: String,
        timetableId: String,
    )

    suspend fun unSetPrimary(
        userId: String,
        timetableId: String,
    )

    suspend fun convertTimetableToTimetableLegacyDto(timetable: Timetable): TimetableLegacyDto

    suspend fun convertTimetableToTimetableDto(timetable: Timetable): TimetableDto
}

@Service
class TimetableServiceImpl(
    private val coursebookService: CoursebookService,
    private val timetableThemeService: TimetableThemeService,
    private val timetableRepository: TimetableRepository,
    private val evRepository: SnuttEvRepository,
    private val dynamicLinkClient: DynamicLinkClient,
    @Value("\${google.firebase.dynamic-link.link-prefix}") val linkPrefix: String,
) : TimetableService {
    override suspend fun getTimetables(userId: String): List<Timetable> = timetableRepository.findAllByUserId(userId).toList()

    override suspend fun getMostRecentlyUpdatedTimetable(userId: String): Timetable =
        timetableRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId) ?: throw TimetableNotFoundException

    override fun getTimetablesBySemester(
        userId: String,
        year: Int,
        semester: Semester,
    ): Flow<Timetable> = timetableRepository.findAllByUserIdAndYearAndSemester(userId, year, semester)

    override suspend fun addTimetable(
        userId: String,
        timetableRequest: TimetableAddRequestDto,
    ): Timetable {
        validateTimetableTitle(userId, timetableRequest.year, timetableRequest.semester, timetableRequest.title)

        val defaultTheme = timetableThemeService.getDefaultTheme(userId)
        return Timetable(
            userId = userId,
            year = timetableRequest.year,
            semester = timetableRequest.semester,
            title = timetableRequest.title,
            theme = defaultTheme.toBasicThemeType(),
            themeId = defaultTheme.toIdForTimetable(),
            isPrimary =
                timetableRepository
                    .findAllByUserIdAndYearAndSemester(userId, timetableRequest.year, timetableRequest.semester)
                    .toList()
                    .isEmpty(),
        ).let { timetableRepository.save(it) }
    }

    override suspend fun getTimetableLink(
        userId: String,
        timetableId: String,
    ): DynamicLinkResponse {
        timetableRepository.findByUserIdAndId(userId, timetableId) ?: throw TimetableNotFoundException
        return dynamicLinkClient.generateLink(
            linkPrefix,
            "snutt://share?timetableId=$timetableId",
        )
    }

    override suspend fun getTimetable(
        userId: String,
        timetableId: String,
    ): Timetable {
        return timetableRepository.findByUserIdAndId(userId, timetableId) ?: throw TimetableNotFoundException
    }

    override suspend fun modifyTimetableTitle(
        userId: String,
        timetableId: String,
        title: String,
    ): Timetable =
        getTimetable(userId, timetableId)
            .also { validateTimetableTitle(userId, it.year, it.semester, title) }
            .apply { this.title = title }
            .let { timetableRepository.save(it) }

    override suspend fun deleteTimetable(
        userId: String,
        timetableId: String,
    ) {
        if (timetableRepository.countAllByUserId(userId) <= 1L) throw TableDeleteErrorException
        getTimetable(userId, timetableId)
        timetableRepository.deleteById(timetableId)
    }

    override suspend fun copyTimetable(
        userId: String,
        timetableId: String,
        title: String?,
    ): Timetable {
        val timetable = timetableRepository.findById(timetableId) ?: throw TimetableNotFoundException
        val baseTitle = (title ?: timetable.title).replace(Regex("""\s\(\d+\)$"""), "")
        val latestCopiedTimetableNumber =
            getLatestCopiedTimetableNumber(userId, timetable.year, timetable.semester, title ?: timetable.title)
        return timetable.copy(
            id = null,
            userId = userId,
            updatedAt = Instant.now(),
            title = baseTitle + " (${latestCopiedTimetableNumber + 1})",
            isPrimary = false,
        ).let { timetableRepository.save(it) }
    }

    override suspend fun modifyTimetableTheme(
        userId: String,
        timetableId: String,
        basicThemeType: BasicThemeType?,
        themeId: String?,
    ): Timetable {
        require((themeId == null) xor (basicThemeType == null))

        val timetable = getTimetable(userId, timetableId)
        val theme = timetableThemeService.getTheme(userId, themeId, basicThemeType)

        timetable.theme = theme.toBasicThemeType()
        timetable.themeId = theme.toIdForTimetable()

        val colorCount = if (theme.isCustom) requireNotNull(theme.colors).size else BasicThemeType.COLOR_COUNT
        timetable.lectures.forEachIndexed { index, lecture ->
            if (theme.isCustom) {
                lecture.color = theme.colors!![index % colorCount]
                lecture.colorIndex = 0
            } else {
                lecture.color = null
                lecture.colorIndex = (index % colorCount) + 1
            }
        }
        return timetableRepository.save(timetable)
    }

    override suspend fun getUserPrimaryTable(
        userId: String,
        year: Int,
        semester: Semester,
    ): Timetable {
        return timetableRepository.findByUserIdAndYearAndSemester(userId, year, semester)
            .toList()
            .ifEmpty { throw TimetableNotFoundException }
            .find { it.isPrimary == true } ?: throw PrimaryTimetableNotFoundException
    }

    override suspend fun getCoursebooksWithPrimaryTable(userId: String): List<CoursebookDto> {
        return timetableRepository.findAllByUserIdAndIsPrimaryTrue(userId)
            .map { CoursebookDto(it.year, it.semester) }
            .toSet()
            .sortedByDescending { it.order }
    }

    override suspend fun createDefaultTable(userId: String): Timetable {
        val coursebook = coursebookService.getLatestCoursebook()
        val defaultTheme = timetableThemeService.getDefaultTheme(userId)

        val timetable =
            Timetable(
                userId = userId,
                year = coursebook.year,
                semester = coursebook.semester,
                title = "나의 시간표",
                theme = defaultTheme.toBasicThemeType(),
                themeId = defaultTheme.toIdForTimetable(),
            )
        return timetableRepository.save(timetable)
    }

    private suspend fun getLatestCopiedTimetableNumber(
        userId: String,
        year: Int,
        semester: Semester,
        title: String,
    ): Int {
        val baseTitle = title.replace(Regex("""\s\(\d+\)$"""), "")
        return timetableRepository.findLatestChildTimetable(userId, year, semester, baseTitle)?.title
            ?.replace(baseTitle, "")?.filter { it.isDigit() }?.toIntOrNull() ?: 0
    }

    private suspend fun validateTimetableTitle(
        userId: String,
        year: Int,
        semester: Semester,
        title: String,
    ) {
        if (title.isEmpty()) {
            throw InvalidTimetableTitleException
        }
        if (timetableRepository.existsByUserIdAndYearAndSemesterAndTitle(userId, year, semester, title)) {
            throw DuplicateTimetableTitleException
        }
    }

    override suspend fun setPrimary(
        userId: String,
        timetableId: String,
    ) {
        val newPrimaryTable =
            timetableRepository.findById(timetableId)
                ?: throw TimetableNotFoundException

        if (newPrimaryTable.isPrimary == true) {
            return
        }

        val primaryTableBefore =
            timetableRepository.findByUserIdAndYearAndSemesterAndIsPrimaryTrue(
                userId,
                newPrimaryTable.year,
                newPrimaryTable.semester,
            )

        if (primaryTableBefore == null) {
            timetableRepository.save(newPrimaryTable.copy(isPrimary = true))
        } else {
            timetableRepository
                .saveAll(listOf(newPrimaryTable.copy(isPrimary = true), primaryTableBefore.copy(isPrimary = false)))
                .collect()
        }
    }

    override suspend fun unSetPrimary(
        userId: String,
        timetableId: String,
    ) {
        val table = timetableRepository.findById(timetableId) ?: throw TimetableNotFoundException
        if (table.isPrimary != true) throw TimetableNotPrimaryException
        timetableRepository.save(table.copy(isPrimary = false))
    }

    override suspend fun convertTimetableToTimetableLegacyDto(timetable: Timetable): TimetableLegacyDto {
        val evLectureIdMap =
            evRepository.getEvIdsBySnuttIds(timetable.lectures.mapNotNull { it.lectureId }).associateBy { it.snuttId }
        val timetableLectures = timetable.lectures.map { TimetableLectureLegacyDto(it, evLectureIdMap[it.lectureId]) }
        return TimetableLegacyDto(timetable, timetableLectures)
    }

    override suspend fun convertTimetableToTimetableDto(timetable: Timetable): TimetableDto {
        val evLectureIdMap =
            evRepository.getEvIdsBySnuttIds(timetable.lectures.mapNotNull { it.lectureId }).associateBy { it.snuttId }
        val timetableLectures = timetable.lectures.map { TimetableLectureDto(it, evLectureIdMap[it.lectureId]) }
        return TimetableDto(timetable, timetableLectures)
    }
}
