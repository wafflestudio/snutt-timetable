package com.wafflestudio.snu4t.sharedtimetable.service

import com.wafflestudio.snu4t.common.exception.NotSharedTimetableOwnerException
import com.wafflestudio.snu4t.common.exception.SharedTimetableAlreadyExistsException
import com.wafflestudio.snu4t.common.exception.SharedTimetableNotFoundException
import com.wafflestudio.snu4t.common.exception.TimetableNotFoundException
import com.wafflestudio.snu4t.sharedtimetable.data.SharedTimeTable
import com.wafflestudio.snu4t.sharedtimetable.dto.SharedTimeTableDetailResponse
import com.wafflestudio.snu4t.sharedtimetable.repository.SharedTimeTableRepository
import com.wafflestudio.snu4t.timetables.repository.TimeTableRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.LocalDateTime

interface SharedTimeTableService {
    suspend fun gets(userId: String): List<SharedTimeTable>
    suspend fun get(sharedTimetableId: String): SharedTimeTableDetailResponse
    suspend fun add(userId: String, title: String, timetableId: String): SharedTimeTable
    suspend fun update(title: String, timetableId: String): SharedTimeTable
    suspend fun delete(userId: String, timetableId: String)
}

@Service
class SharedTimeTableServiceImpl(
    private val timetableRepository: TimeTableRepository,
    private val sharedTimetableRepository: SharedTimeTableRepository,
) : SharedTimeTableService {
    override suspend fun gets(userId: String): List<SharedTimeTable> {
        val sharedTimetables = sharedTimetableRepository.findAllByUserIdAndIsDeletedFalse(userId)
        val timetableIds = sharedTimetables.map { it.timetableId }
        val validTimetableIds = timetableRepository.findAllById(timetableIds).toList().map { it.id }
        return sharedTimetables
            .filter { validTimetableIds.contains(it.timetableId) }
    }
    override suspend fun get(sharedTimetableId: String): SharedTimeTableDetailResponse {
        val sharedTimeTable = sharedTimetableRepository.findSharedTimeTableByIdAndIsDeletedFalse(sharedTimetableId) ?: throw SharedTimetableNotFoundException
        // TODO: 시간표 삭제시 공유시간표도 같이 삭제하고 로그 찍기
        val timetable = timetableRepository.findById(sharedTimeTable.timetableId) ?: throw TimetableNotFoundException
        return SharedTimeTableDetailResponse(
            id = sharedTimetableId,
            userId = sharedTimeTable.userId,
            title = sharedTimeTable.title,
            timetable = timetable,
        )
    }
    override suspend fun add(userId: String, title: String, timetableId: String): SharedTimeTable {
        val timetable = timetableRepository.findById(timetableId) ?: throw TimetableNotFoundException
        val existingTimetableIds = sharedTimetableRepository.findAllByUserIdAndIsDeletedFalse(userId).map { it.timetableId }
        if (existingTimetableIds.contains(timetableId)) {
            throw SharedTimetableAlreadyExistsException
        }
        return sharedTimetableRepository.save(
            SharedTimeTable(
                userId = userId,
                title = title,
                timetableId = timetableId,
            )
        )
    }

    override suspend fun update(title: String, timetableId: String): SharedTimeTable {
        val sharedTimetable = sharedTimetableRepository.findSharedTimeTableByIdAndIsDeletedFalse(timetableId) ?: throw SharedTimetableNotFoundException
        sharedTimetable.title = title
        sharedTimetable.updatedAt = LocalDateTime.now()
        sharedTimetableRepository.save(sharedTimetable)
        return sharedTimetable
    }

    override suspend fun delete(userId: String, timetableId: String) {
        val sharedTimetable = sharedTimetableRepository.findSharedTimeTableByIdAndIsDeletedFalse(timetableId) ?: throw SharedTimetableNotFoundException
        if (sharedTimetable.userId != userId) {
            throw NotSharedTimetableOwnerException
        }
        sharedTimetable.isDeleted = true
        sharedTimetableRepository.save(sharedTimetable)
    }
}
