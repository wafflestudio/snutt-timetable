package com.wafflestudio.snu4t.common.enum

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.data.domain.Sort

enum class SortCriteria(
    val value: Int,
    @JsonValue
    val fullName: String
) {
    ID(1, "기본값"),
    RATING_DESC(2, "평점 높은 순"),
    RATING_ASC(3, "평점 낮은 순"),
    COUNT_DESC(4, "강의평 많은 순"),
    COUNT_ASC(5, "강의평 적은 순");

    companion object {
        fun getSort(sortCriteria: SortCriteria?): Sort = when (sortCriteria) {
            RATING_DESC -> Sort.by("evInfo.avgRating").descending()
            RATING_ASC -> Sort.by("evInfo.avgRating").ascending()
            COUNT_DESC -> Sort.by("evInfo.count").descending()
            COUNT_ASC -> Sort.by("evInfo.count").ascending()
            else -> Sort.unsorted()
        }
    }
}
