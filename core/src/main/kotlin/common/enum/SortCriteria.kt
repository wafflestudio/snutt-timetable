package com.wafflestudio.snu4t.common.enum

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.data.domain.Sort

enum class SortCriteria(
    val value: Int,
    @JsonValue
    val fullName: String
) {
    ID(1, "기본값"),
    RATING(2, "평점순");

    companion object {
        fun getSort(sortCriteria: SortCriteria?): Sort = when (sortCriteria) {
            RATING -> Sort.by("evInfo.avgRating").descending()
            else -> Sort.unsorted()
        }
    }
}
