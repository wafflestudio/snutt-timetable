package com.wafflestudio.snu4t.lectures.utils

import com.wafflestudio.snu4t.lectures.data.ClassTime
import kotlin.math.ceil
import kotlin.math.floor

object ClassTimeUtils {

    // FIXME: 바로 다음에 없애야 하는 스펙
    fun classTimeToBitmask(classTimes: List<ClassTime>): List<Int> {
        val bitTable = Array(7) { Array(30) { 0 } }

        classTimes.map { classTime ->
            val dayValue = classTime.day.value
            val startPeriod = floor((classTime.startMinute.toDouble() - 480) / 30) / 2
            val endPeriod = ceil((classTime.endMinute.toDouble() - 480) / 30) / 2
            for (i: Int in (startPeriod * 2).toInt() until (endPeriod * 2).toInt())
                bitTable[dayValue][i] = 1
        }

        return bitTable.map { day -> day.reduce { res, i -> res.shl(1) + i } }
    }

    fun parseMinute(classTime: String) =
        classTime.split(":").let { (hour, minute) -> hour.toInt() * 60 + minute.toInt() }

    fun timesOverlap(times1: List<ClassTime>, times2: List<ClassTime>) =
        times1.any { classTime1 ->
            times2.any { classTime2 ->
                twoTimesOverlap(classTime1, classTime2)
            }
        }

    fun twoTimesOverlap(time1: ClassTime, time2: ClassTime) =
        time1.day == time2.day &&
            time1.startMinute < time2.endMinute && time1.endMinute > time2.startMinute
}
