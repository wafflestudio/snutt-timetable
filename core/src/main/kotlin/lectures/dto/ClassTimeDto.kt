package com.wafflestudio.snu4t.lectures.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.wafflestudio.snu4t.common.enum.DayOfWeek
import com.wafflestudio.snu4t.lectures.data.ClassTime
import com.wafflestudio.snu4t.lectures.utils.endPeriod
import com.wafflestudio.snu4t.lectures.utils.startPeriod

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ClassTimeDto(
    val day: DayOfWeek,
    val place: String?,
    val startMinute: Int,
    val endMinute: Int,
    val startTime: String,
    val endTime: String,
    @JsonProperty("len")
    val periodLength: Double,
    @JsonProperty("start")
    val startPeriod: Double,
)

fun ClassTimeDto(classTime: ClassTime): ClassTimeDto = ClassTimeDto(
    day = classTime.day,
    place = classTime.place,
    startMinute = classTime.startMinute,
    endMinute = classTime.endMinute,
    startTime = "${classTime.startMinute / 60}:${classTime.startMinute % 60}",
    endTime = "${classTime.endMinute / 60}:${classTime.endMinute % 60}",
    startPeriod = classTime.startPeriod,
    periodLength = classTime.endPeriod - classTime.startPeriod,
)
