package com.wafflestudio.snu4t.lectures.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SearchLectureRequest(
    val year: Int,
    val semester: Int,
    val query: SearchLectureQuery?,
)

data class SearchLectureQuery(
    val year:Int,
    val semester: Int,
    val title: String?,
    val classification: List<String>?,
    val credit: List<Int>?,
    val courseNumber: List<String>?,
    val academicYear: List<String>?,
    val instructor: List<String>?,
    val department: List<String>?,
    val category: List<String>?,
    val timeMask: List<Int>?,
    val etc: List<String>?,
    val limit: Int?,
    val offset: Int?,
)
