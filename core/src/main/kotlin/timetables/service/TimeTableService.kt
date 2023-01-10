package com.wafflestudio.snu4t.timetables.service

import com.wafflestudio.snu4t.timetables.repository.TimeTableRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import timetables.dto.TimeTableBriefDto

interface TimeTableService {
    suspend fun getBriefs(userId: String): List<TimeTableBriefDto>
}

@Service
class TimeTableServiceImpl(
    private val timeTableRepository: TimeTableRepository,
) : TimeTableService {
    override suspend fun getBriefs(userId: String): List<TimeTableBriefDto> =
        timeTableRepository.findAllByUserId(userId)
            .map(::TimeTableBriefDto)
            .toList()
}
