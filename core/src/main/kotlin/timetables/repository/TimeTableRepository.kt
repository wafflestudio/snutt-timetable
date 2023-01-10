package com.wafflestudio.snu4t.timetables.repository

import com.wafflestudio.snu4t.timetables.data.TimeTable
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TimeTableRepository : CoroutineCrudRepository<TimeTable, String> {
    fun findAllByUserId(userId: String): Flow<TimeTable>
}
