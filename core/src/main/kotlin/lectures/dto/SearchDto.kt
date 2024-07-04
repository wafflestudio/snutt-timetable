package com.wafflestudio.snu4t.lectures.dto

import com.wafflestudio.snu4t.common.enum.Semester
import org.springframework.data.domain.Sort

data class SearchDto(
    val year: Int,
    val semester: Semester,
    val query: String? = null,
    val classification: List<String>? = null,
    val credit: List<Int>? = null,
    val courseNumber: List<String>? = null,
    val academicYear: List<String>? = null,
    val department: List<String>? = null,
    val category: List<String>? = null,
    val etcTags: List<String>? = null,
    val times: List<SearchTimeDto>? = null,
    val timesToExclude: List<SearchTimeDto>? = null,
    val page: Int = 0,
    val offset: Long = page * 20L,
    val limit: Int = 20,
    val sortBy: Sort,
)
