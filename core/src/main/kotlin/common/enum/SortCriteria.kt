package com.wafflestudio.snu4t.common.enum

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.data.domain.Sort

enum class SortCriteria(
    @JsonValue
    val value: Int,
    val fullName: String
) {
    ID(1, "기본값"),
    RATING(2, "평점");

    companion object {
        fun getSort(sortCriteria: SortCriteria?): Sort = when (sortCriteria) {
            RATING -> Sort.by("evInfo.avgRating").descending()
            else -> Sort.unsorted()
        }
        fun from(name: String?) = SortCriteria.values().find { it.name == name }
    }
}
