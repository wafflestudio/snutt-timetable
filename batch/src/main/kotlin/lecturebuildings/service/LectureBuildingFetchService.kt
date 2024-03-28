package com.wafflestudio.snu4t.lecturebuildings.service

import com.wafflestudio.snu4t.lecturebuildings.SnuMapRepository
import com.wafflestudio.snu4t.lecturebuildings.data.Campus
import com.wafflestudio.snu4t.lecturebuildings.data.GeoCoordinate
import com.wafflestudio.snu4t.lecturebuildings.data.LectureBuilding
import com.wafflestudio.snu4t.lecturebuildings.data.SnuMapSearchItem
import com.wafflestudio.snu4t.lecturebuildings.data.SnuMapSearchResult
import com.wafflestudio.snu4t.lecturebuildings.repository.LectureBuildingRepository
import org.springframework.stereotype.Service

interface LectureBuildingFetchService {
    suspend fun getSnuMapLectureBuilding(campus: Campus, buildingNumber: String): LectureBuilding?
}

@Service
class LectureBuildingFetchServiceImpl(
    private val snuMapRepository: SnuMapRepository,
    private val lectureBuildingRepository: LectureBuildingRepository
) : LectureBuildingFetchService {
    override suspend fun getSnuMapLectureBuilding(
        campus: Campus,
        buildingNumber: String
    ): LectureBuilding? = lectureBuildingRepository.findByBuildingNumber(buildingNumber)
        ?: selectMostProbableSearchItem(buildingNumber, snuMapRepository.getLectureBuildingSearchResult(buildingNumber))
            ?.let {
                lectureBuildingRepository.save(
                    LectureBuilding(
                        buildingNumber = it.buildingNumber!!,
                        buildingNameKor = it.name,
                        buildingNameEng = it.englishName,
                        locationInDMS = GeoCoordinate(it.latitudeInDMS, it.longitudeInDMS),
                        locationInDecimal = GeoCoordinate(it.latitudeInDecimal, it.longitudeInDecimal),
                        campus = campus
                    )
                )
            }

    private fun selectMostProbableSearchItem(
        buildingNumber: String,
        searchResult: SnuMapSearchResult
    ): SnuMapSearchItem? = searchResult.searchList
        .filter { it.contentType == "F" && it.facType == "OTHER" && it.buildingNumber == buildingNumber }
        .minByOrNull { it.name.count() }
}
