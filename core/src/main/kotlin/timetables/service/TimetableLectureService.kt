package com.wafflestudio.snu4t.timetables.service

import com.wafflestudio.snu4t.common.exception.CustomLectureResetException
import com.wafflestudio.snu4t.common.exception.DuplicateTimetableLectureException
import com.wafflestudio.snu4t.common.exception.InvalidTimeException
import com.wafflestudio.snu4t.common.exception.LectureNotFoundException
import com.wafflestudio.snu4t.common.exception.LectureTimeOverlapException
import com.wafflestudio.snu4t.common.exception.TimetableNotFoundException
import com.wafflestudio.snu4t.common.exception.WrongSemesterException
import com.wafflestudio.snu4t.lectures.repository.LectureRepository
import com.wafflestudio.snu4t.lectures.utils.ClassTimeUtils
import com.wafflestudio.snu4t.theme.service.TimetableThemeService
import com.wafflestudio.snu4t.timetables.data.Timetable
import com.wafflestudio.snu4t.timetables.data.TimetableLecture
import com.wafflestudio.snu4t.timetables.dto.request.CustomTimetableLectureAddLegacyRequestDto
import com.wafflestudio.snu4t.timetables.dto.request.TimetableLectureModifyLegacyRequestDto
import com.wafflestudio.snu4t.timetables.repository.TimetableRepository
import org.springframework.stereotype.Service

interface TimetableLectureService {
    suspend fun addLecture(
        userId: String,
        timetableId: String,
        lectureId: String,
        isForced: Boolean,
    ): Timetable

    suspend fun addCustomTimetableLecture(
        userId: String,
        timetableId: String,
        timetableLectureRequest: CustomTimetableLectureAddLegacyRequestDto,
        isForced: Boolean,
    ): Timetable

    suspend fun resetTimetableLecture(
        userId: String,
        timetableId: String,
        timetableLectureId: String,
        isForced: Boolean,
    ): Timetable

    suspend fun modifyTimetableLecture(
        userId: String,
        timetableId: String,
        timetableLectureId: String,
        modifyTimetableLectureRequestDto: TimetableLectureModifyLegacyRequestDto,
        isForced: Boolean,
    ): Timetable

    suspend fun deleteTimetableLecture(
        userId: String,
        timetableId: String,
        timetableLectureId: String,
    ): Timetable
}

@Service
class TimetableLectureServiceImpl(
    private val timetableThemeService: TimetableThemeService,
    private val timetableRepository: TimetableRepository,
    private val lectureRepository: LectureRepository,
) : TimetableLectureService {
    override suspend fun addLecture(
        userId: String,
        timetableId: String,
        lectureId: String,
        isForced: Boolean,
    ): Timetable {
        val timetable = timetableRepository.findByUserIdAndId(userId, timetableId) ?: throw TimetableNotFoundException
        val lecture = lectureRepository.findById(lectureId) ?: throw LectureNotFoundException
        if (!(timetable.year == lecture.year && timetable.semester == lecture.semester)) throw WrongSemesterException

        val (colorIndex, color) = timetableThemeService.getNewColorIndexAndColor(timetable)

        if (timetable.lectures.any { it.lectureId == lectureId }) throw DuplicateTimetableLectureException
        val timetableLecture = TimetableLecture(lecture, colorIndex, color)
        resolveTimeConflict(timetable, timetableLecture, isForced)
        return timetableRepository.pushTimetableLecture(timetable.id!!, timetableLecture)
    }

    override suspend fun addCustomTimetableLecture(
        userId: String,
        timetableId: String,
        timetableLectureRequest: CustomTimetableLectureAddLegacyRequestDto,
        isForced: Boolean,
    ): Timetable {
        val timetable = timetableRepository.findByUserIdAndId(userId, timetableId) ?: throw TimetableNotFoundException
        val timetableLecture = timetableLectureRequest.toTimetableLecture()
        if (ClassTimeUtils.timesOverlap(timetableLecture.classPlaceAndTimes)) throw InvalidTimeException
        resolveTimeConflict(timetable, timetableLecture, isForced)
        return timetableRepository.pushTimetableLecture(timetable.id!!, timetableLecture)
    }

    override suspend fun resetTimetableLecture(
        userId: String,
        timetableId: String,
        timetableLectureId: String,
        isForced: Boolean,
    ): Timetable {
        val timetable = timetableRepository.findByUserIdAndId(userId, timetableId) ?: throw TimetableNotFoundException
        val timetableLecture = timetable.lectures.find { it.id == timetableLectureId } ?: throw LectureNotFoundException
        val originalLectureId = timetableLecture.lectureId ?: throw CustomLectureResetException
        val originalLecture = lectureRepository.findById(originalLectureId) ?: throw LectureNotFoundException
        timetableLecture.apply {
            courseTitle = originalLecture.courseTitle
            instructor = originalLecture.instructor
            classification = originalLecture.classification
            department = originalLecture.department
            credit = originalLecture.credit
            remark = originalLecture.remark
            classPlaceAndTimes = originalLecture.classPlaceAndTimes
        }
        resolveTimeConflict(timetable, timetableLecture, isForced)
        return timetableRepository.updateTimetableLecture(timetableId, timetableLecture)
    }

    override suspend fun modifyTimetableLecture(
        userId: String,
        timetableId: String,
        timetableLectureId: String,
        modifyTimetableLectureRequestDto: TimetableLectureModifyLegacyRequestDto,
        isForced: Boolean,
    ): Timetable {
        val timetable = timetableRepository.findByUserIdAndId(userId, timetableId) ?: throw TimetableNotFoundException
        val timetableLecture = timetable.lectures.find { it.id == timetableLectureId } ?: throw LectureNotFoundException
        val newClassPlaceAndTimes =
            modifyTimetableLectureRequestDto.classPlaceAndTimes?.map { it.toClassPlaceAndTime() }
                ?: timetableLecture.classPlaceAndTimes
        if (ClassTimeUtils.timesOverlap(newClassPlaceAndTimes)) throw InvalidTimeException
        timetableLecture.apply {
            courseTitle = modifyTimetableLectureRequestDto.courseTitle ?: courseTitle
            academicYear = modifyTimetableLectureRequestDto.academicYear ?: academicYear
            category = modifyTimetableLectureRequestDto.category ?: category
            classification = modifyTimetableLectureRequestDto.classification ?: classification
            instructor = modifyTimetableLectureRequestDto.instructor ?: instructor
            credit = modifyTimetableLectureRequestDto.credit ?: credit
            remark = modifyTimetableLectureRequestDto.remark ?: remark
            color = modifyTimetableLectureRequestDto.color ?: color
            colorIndex = modifyTimetableLectureRequestDto.colorIndex ?: colorIndex
            classPlaceAndTimes = newClassPlaceAndTimes
        }
        resolveTimeConflict(timetable, timetableLecture, isForced)
        return timetableRepository.updateTimetableLecture(timetableId, timetableLecture)
    }

    override suspend fun deleteTimetableLecture(
        userId: String,
        timetableId: String,
        timetableLectureId: String,
    ): Timetable {
        timetableRepository.findByUserIdAndId(userId, timetableId) ?: throw TimetableNotFoundException
        return timetableRepository.pullTimetableLecture(timetableId, timetableLectureId)
    }

    private suspend fun resolveTimeConflict(
        timetable: Timetable,
        timetableLecture: TimetableLecture,
        isForced: Boolean,
    ) {
        val overlappingLectures =
            timetable.lectures.filter {
                timetableLecture.id != it.id &&
                    ClassTimeUtils.timesOverlap(
                        timetableLecture.classPlaceAndTimes, it.classPlaceAndTimes,
                    )
            }
        when {
            overlappingLectures.isNotEmpty() && !isForced -> {
                val confirmMessage = makeOverwritingConfirmMessage(overlappingLectures)
                throw LectureTimeOverlapException(confirmMessage)
            }

            overlappingLectures.isNotEmpty() && isForced -> {
                timetableRepository.pullTimetableLectures(timetable.id!!, overlappingLectures.map { it.id })
            }
        }
    }

    private fun makeOverwritingConfirmMessage(overlappingLectures: List<TimetableLecture>): String {
        val overlappingLectureTitles =
            overlappingLectures.map { "'${it.courseTitle}'" }.take(2).joinToString(", ")
        val shortFormOfTitles = if (overlappingLectures.size < 3) "" else "외 ${overlappingLectures.size - 2}개의 "
        return "$overlappingLectureTitles ${shortFormOfTitles}강의와 시간이 겹칩니다. 강의를 덮어쓰시겠습니까?"
    }
}
