package com.wafflestudio.snu4t.lectures.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.wafflestudio.snu4t.evaluation.dto.SnuttEvLectureSummaryDto
import com.wafflestudio.snu4t.lectures.data.BookmarkLecture
import com.wafflestudio.snu4t.lectures.utils.ClassTimeUtils

data class BookmarkLectureDto(
    @JsonProperty("_id")
    var id: String? = null,
    @JsonProperty("academic_year")
    var academicYear: String?,
    var category: String?,
    @JsonProperty("class_time_json")
    var classTimes: List<ClassPlaceAndTimeLegacyDto>,
    var classification: String?,
    var credit: Long,
    var department: String?,
    var instructor: String?,
    @JsonProperty("lecture_number")
    var lectureNumber: String,
    var quota: Int?,
    var freshmanQuota: Int?,
    var remark: String?,
    @JsonProperty("course_number")
    var courseNumber: String,
    @JsonProperty("course_title")
    var courseTitle: String,
    val snuttEvLecture: SnuttEvLectureSummaryDto? = null,

    // FIXME: 안드로이드 구버전 대응용 필드 1년 후 2024년에 삭제 (2023/06/26)
    @JsonProperty("class_time_mask")
    val classTimeMask: List<Int> = emptyList(),
)

fun BookmarkLectureDto(lecture: BookmarkLecture, snuttEvLecture: SnuttEvLectureSummaryDto? = null): BookmarkLectureDto = BookmarkLectureDto(
    id = lecture.id,
    academicYear = lecture.academicYear,
    category = lecture.category,
    classTimes = lecture.classPlaceAndTimes.map { ClassPlaceAndTimeLegacyDto(it) },
    classification = lecture.classification,
    credit = lecture.credit,
    department = lecture.department,
    instructor = lecture.instructor,
    quota = lecture.quota,
    freshmanQuota = lecture.freshmanQuota,
    remark = lecture.remark,
    lectureNumber = lecture.lectureNumber,
    courseNumber = lecture.courseNumber,
    courseTitle = lecture.courseTitle,
    snuttEvLecture = snuttEvLecture,
    classTimeMask = ClassTimeUtils.classTimeToBitmask(lecture.classPlaceAndTimes),
)
