package com.wafflestudio.snu4t.lecturebuildings.service

import com.wafflestudio.snu4t.lecturebuildings.data.Campus
import com.wafflestudio.snu4t.lecturebuildings.data.LectureBuilding
import com.wafflestudio.snu4t.lecturebuildings.data.LectureBuildingUpdateResult
import com.wafflestudio.snu4t.lecturebuildings.data.PlaceInfo
import com.wafflestudio.snu4t.lecturebuildings.repository.LectureBuildingRepository
import com.wafflestudio.snu4t.lectures.data.Lecture
import com.wafflestudio.snu4t.lectures.repository.LectureRepository
import com.wafflestudio.snu4t.timetables.data.Timetable
import com.wafflestudio.snu4t.timetables.repository.TimetableRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant

interface LectureBuildingPopulateService {
    suspend fun fetchMissingBuildingInfo(lectures: List<Lecture>)
    suspend fun populateLectureBuildingsOfTimetables(lecture: Lecture): List<Timetable>
    suspend fun updateBuildingInfoOfLectures(lectures: List<Lecture>): LectureBuildingUpdateResult
}

@Service
class LectureBuildingPopulateServiceImpl(
    private val lectureBuildingFetchService: LectureBuildingFetchService,
    private val lectureRepository: LectureRepository,
    private val lectureBuildingRepository: LectureBuildingRepository,
    private val timetableRepository: TimetableRepository,
) : LectureBuildingPopulateService {
    // 빌딩 정보를 조회하고 없으면 크롤링해옴
    override suspend fun fetchMissingBuildingInfo(lectures: List<Lecture>) {
        val lecturePlaceInfos = lectures.extractPlaceInfos()

        val lectureBuildingNumbers = lecturePlaceInfos.map { it.buildingNumber }.toSet()
        val savedLectureBuildings = lectureBuildingRepository.findByBuildingNumberIsIn(lectureBuildingNumbers)
        val savedBuildingNumbers = savedLectureBuildings.map { it.buildingNumber }.toSet()

        // 존재하지 않는 강의동을 캠퍼스맵에서 긁어와서 저장
        val buildingNumbersToFetch = lectureBuildingNumbers - savedBuildingNumbers
        val fetchedBuildlingInfo = fetchBuildings(buildingNumbersToFetch)
        lectureBuildingRepository.saveAll(fetchedBuildlingInfo).collect()
    }

    private suspend fun fetchBuildings(buildingNumbers: Set<String>): List<LectureBuilding> = coroutineScope {
        buildingNumbers.map {
            async { lectureBuildingFetchService.getSnuMapLectureBuilding(Campus.GWANAK, it) }
        }.awaitAll()
    }.filterNotNull()

    override suspend fun updateBuildingInfoOfLectures(lectures: List<Lecture>): LectureBuildingUpdateResult {
        val lectureBuildings = lectureBuildingRepository.findAll().toList()
        val placeInfos = lectures.extractPlaceInfos()
        val buildingNumberToBuildingMap = lectureBuildings.associateBy { it.buildingNumber }
        val placeStringToBuildingMap = placeInfos.associate { it.rawString to buildingNumberToBuildingMap[it.buildingNumber] }
        return lectures.fold(LectureBuildingUpdateResult(mutableListOf(), mutableListOf(), mutableListOf())) { acc, lecture ->
            lecture.classPlaceAndTimes.forEach { classPlaceAndTime ->
                classPlaceAndTime.apply {
                    this.lectureBuildings =
                        PlaceInfo.getValuesOf(classPlaceAndTime.place).mapNotNull { placeStringToBuildingMap[it.rawString] }
                }
            }

            when {
                lecture.classPlaceAndTimes.all { it.place.isBlank() } -> LectureBuildingUpdateResult(
                    acc.lecturesWithBuildingInfos,
                    acc.lecturesWithOutBuildingInfos + lecture,
                    acc.lecturesFailed,
                )
                lecture.classPlaceAndTimes.any { it.place.isNotBlank() and it.lectureBuildings.isNullOrEmpty() } -> LectureBuildingUpdateResult(
                    acc.lecturesWithBuildingInfos,
                    acc.lecturesWithOutBuildingInfos,
                    acc.lecturesFailed + lecture,
                )
                else -> LectureBuildingUpdateResult(
                    acc.lecturesWithBuildingInfos + lecture,
                    acc.lecturesWithOutBuildingInfos,
                    acc.lecturesFailed,
                )
            }

        }.also { lectureRepository.saveAll(it.lecturesWithBuildingInfos) }
    }

    override suspend fun populateLectureBuildingsOfTimetables(lecture: Lecture): List<Timetable> =
        timetableRepository.findAllContainsLectureId(lecture.year, lecture.semester, lecture.id!!)
            .filter { timetable -> timetable.lectures.any { it.lectureId == lecture.id && it.classPlaceAndTimes == lecture.classPlaceAndTimes } }
            .map { timetable ->
                timetable.apply {
                    lectures.find { it.lectureId == lecture.id && it.classPlaceAndTimes == lecture.classPlaceAndTimes }
                        ?.apply {
                            classPlaceAndTimes = lecture.classPlaceAndTimes
                        }
                    updatedAt = Instant.now()
                }
            }.let { timetableRepository.saveAll(it) }.toList()

    private fun List<Lecture>.extractPlaceInfos() =
        this.flatMap { it.classPlaceAndTimes }
            .map { it.place }.distinct()
            .flatMap { PlaceInfo.getValuesOf(it) }
}
