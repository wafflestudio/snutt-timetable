package com.wafflestudio.snu4t.handler

import com.fasterxml.jackson.annotation.JsonProperty
import com.wafflestudio.snu4t.common.enum.DayOfWeek
import com.wafflestudio.snu4t.common.enum.Semester
import com.wafflestudio.snu4t.common.enum.SortCriteria
import com.wafflestudio.snu4t.lectures.dto.SearchDto
import com.wafflestudio.snu4t.lectures.dto.SearchTimeDto
import com.wafflestudio.snu4t.lectures.service.LectureService
import com.wafflestudio.snu4t.middleware.SnuttRestApiNoAuthMiddleware
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody

@Component
class LectureSearchHandler(
    private val lectureService: LectureService,
    snuttRestApiNoAuthMiddleware: SnuttRestApiNoAuthMiddleware,
) : ServiceHandler(
    handlerMiddleware = snuttRestApiNoAuthMiddleware
) {
    suspend fun searchLectures(req: ServerRequest): ServerResponse = handle(req) {
        val query: SearchQueryLegacy = req.awaitBody()
        lectureService.search(query.toSearchDto()).toList().let { lectureService.convertLecturesToLectureDtos(it) }
    }
}

data class SearchQueryLegacy(
    val year: Int,
    val semester: Semester,
    val title: String? = null,
    val classification: List<String>? = null,
    val credit: List<Int>? = null,
    @JsonProperty("course_number")
    val courseNumber: List<String>? = null,
    @JsonProperty("academic_year")
    val academicYear: List<String>? = null,
    val department: List<String>? = null,
    val category: List<String>? = null,
    @JsonProperty("time_mask")
    val timeMask: List<Int>? = null,
    val times: List<SearchTimeDto>? = null,
    val timesToExclude: List<SearchTimeDto>? = null,
    val etc: List<String>? = null,
    val page: Int = 0,
    val offset: Long = page * 20L,
    val limit: Int = 20,
    val sortCriteria: String? = null,
) {
    fun toSearchDto(): SearchDto {
        return SearchDto(
            year = year,
            semester = semester,
            query = title,
            classification = classification,
            credit = credit,
            courseNumber = courseNumber,
            academicYear = academicYear,
            department = department,
            category = category,
            etcTags = etc,
            times = times?.takeIf { it.isNotEmpty() } ?: bitmaskToClassTime(timeMask),
            timesToExclude = timesToExclude,
            page = page,
            offset = offset,
            limit = limit,
            sortBy = SortCriteria.getSort(SortCriteria.from(sortCriteria))
        )
    }

    /*
    기존 timeMask 스펙을 대응하기 위한 코드
    8시부터 23시까지 30분 단위로 강의가 존재하는 경우 1, 존재하지 않는 경우 0인 2진수가 전달된다.
    이를 searchTimeDto로 변환
    ex) 111000... -> 8:00~9:30 수업 -> startMinute = 480, endMinute: 570
     */
    private fun bitmaskToClassTime(timeMask: List<Int>?): List<SearchTimeDto>? =
        timeMask?.flatMapIndexed { dayValue, mask ->
            mask.toTimeMask().zip((mask shr 1).toTimeMask())
                .foldIndexed(emptyList()) { index, acc, (bit, bitBefore) ->
                    if (bit && !bitBefore) {
                        acc + SearchTimeDto(
                            day = DayOfWeek.getOfValue(dayValue)!!,
                            startMinute = index * 30 + 8 * 60,
                            endMinute = index * 30 + 8 * 60 + 30
                        )
                    } else if (bit && bitBefore) {
                        val updated = acc.last().copy(endMinute = index * 30 + 8 * 60 + 30)
                        acc.dropLast(1) + updated
                    } else {
                        acc
                    }
                }
        }

    /*
    ex) 258048 -> 2진수 00000000000000111111000000000000 -> [..., true, true, true, true, true, true, false, false, ...]
     */
    private fun Int.toTimeMask(): List<Boolean> = this.toString(2).padStart(30, '0').map { it == '1' }
}
