package com.wafflestudio.snu4t.sugangsnu.job.sync.service

import com.wafflestudio.snu4t.bookmark.repository.BookmarkRepository
import com.wafflestudio.snu4t.common.cache.Cache
import com.wafflestudio.snu4t.common.enum.SortCriteria
import com.wafflestudio.snu4t.coursebook.data.Coursebook
import com.wafflestudio.snu4t.coursebook.repository.CoursebookRepository
import com.wafflestudio.snu4t.lecturebuildings.data.Campus
import com.wafflestudio.snu4t.lecturebuildings.data.PlaceInfo
import com.wafflestudio.snu4t.lecturebuildings.service.LectureBuildingService
import com.wafflestudio.snu4t.lectures.data.Lecture
import com.wafflestudio.snu4t.lectures.service.LectureService
import com.wafflestudio.snu4t.lectures.utils.ClassTimeUtils
import com.wafflestudio.snu4t.sugangsnu.common.SugangSnuRepository
import com.wafflestudio.snu4t.sugangsnu.common.data.SugangSnuCoursebookCondition
import com.wafflestudio.snu4t.sugangsnu.common.service.SugangSnuFetchService
import com.wafflestudio.snu4t.sugangsnu.job.sync.data.BookmarkLectureDeleteResult
import com.wafflestudio.snu4t.sugangsnu.job.sync.data.BookmarkLectureUpdateResult
import com.wafflestudio.snu4t.sugangsnu.job.sync.data.SugangSnuLectureCompareResult
import com.wafflestudio.snu4t.sugangsnu.job.sync.data.TimetableLectureDeleteByOverlapResult
import com.wafflestudio.snu4t.sugangsnu.job.sync.data.TimetableLectureDeleteResult
import com.wafflestudio.snu4t.sugangsnu.job.sync.data.TimetableLectureUpdateResult
import com.wafflestudio.snu4t.sugangsnu.job.sync.data.UpdatedLecture
import com.wafflestudio.snu4t.sugangsnu.job.sync.data.UserLectureSyncResult
import com.wafflestudio.snu4t.tag.data.TagCollection
import com.wafflestudio.snu4t.tag.data.TagList
import com.wafflestudio.snu4t.tag.repository.TagListRepository
import com.wafflestudio.snu4t.timetables.data.Timetable
import com.wafflestudio.snu4t.timetables.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.reflect.full.memberProperties

interface SugangSnuSyncService {
    suspend fun updateCoursebook(coursebook: Coursebook): List<UserLectureSyncResult>
    suspend fun addCoursebook(coursebook: Coursebook)
    suspend fun isSyncWithSugangSnu(latestCoursebook: Coursebook): Boolean
    suspend fun flushCache()
}

@Service
class SugangSnuSyncServiceImpl(
    private val sugangSnuFetchService: SugangSnuFetchService,
    private val lectureService: LectureService,
    private val timeTableRepository: TimetableRepository,
    private val sugangSnuRepository: SugangSnuRepository,
    private val coursebookRepository: CoursebookRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val tagListRepository: TagListRepository,
    private val lectureBuildingService: LectureBuildingService,
    private val cache: Cache,
) : SugangSnuSyncService {
    override suspend fun updateCoursebook(coursebook: Coursebook): List<UserLectureSyncResult> {
        val newLectures = sugangSnuFetchService.getSugangSnuLectures(coursebook.year, coursebook.semester)
        val oldLectures =
            lectureService.getLecturesByYearAndSemesterAsFlow(coursebook.year, coursebook.semester).toList()
        val compareResult = compareLectures(newLectures, oldLectures)

        syncLectures(compareResult)
        updateLectureBuildings(compareResult)
        val syncUserLecturesResults = syncSavedUserLectures(compareResult)
        syncTagList(coursebook, newLectures)
        coursebookRepository.save(coursebook.apply { updatedAt = Instant.now() })

        return syncUserLecturesResults
    }

    override suspend fun addCoursebook(coursebook: Coursebook) {
        val newLectures = sugangSnuFetchService.getSugangSnuLectures(coursebook.year, coursebook.semester)
        lectureService.upsertLectures(newLectures)
        syncTagList(coursebook, newLectures)

        coursebookRepository.save(coursebook)
    }

    override suspend fun isSyncWithSugangSnu(latestCoursebook: Coursebook): Boolean {
        val sugangSnuLatestCoursebook = sugangSnuRepository.getCoursebookCondition()
        return latestCoursebook.isSyncedToSugangSnu(sugangSnuLatestCoursebook)
    }

    override suspend fun flushCache() = cache.flushDatabase()

    private fun compareLectures(
        newLectures: Iterable<Lecture>,
        oldLectures: Iterable<Lecture>
    ): SugangSnuLectureCompareResult {
        val newMap = newLectures.associateBy { lecture -> lecture.courseNumber + "##" + lecture.lectureNumber }
        val oldMap = oldLectures.associateBy { lecture -> lecture.courseNumber + "##" + lecture.lectureNumber }

        val created = (newMap.keys - oldMap.keys).map(newMap::getValue)
        val updated = (newMap.keys intersect oldMap.keys)
            .map { oldMap[it]!! to newMap[it]!! }
            .filter { (old, new) -> !(old equalsMetadata new) }
            .map { (old, new) ->
                UpdatedLecture(
                    old, new,
                    Lecture::class.memberProperties.filter {
                        it != Lecture::id && it.get(old) != it.get(new)
                    }
                )
            }
        val deleted = (oldMap.keys - newMap.keys).map(
            oldMap::getValue
        )

        return SugangSnuLectureCompareResult(created, deleted, updated)
    }

    private suspend fun syncTagList(coursebook: Coursebook, lectures: Iterable<Lecture>) {
        val tagCollection = lectures.fold(ParsedTags()) { acc, lecture ->
            ParsedTags(
                academicYear = acc.academicYear + lecture.academicYear,
                classification = acc.classification + lecture.classification,
                department = acc.department + lecture.department,
                credit = acc.credit + lecture.credit,
                instructor = acc.instructor + lecture.instructor,
                category = acc.category + lecture.category,
            )
        }.let { parsedTag ->
            TagCollection(
                // 엑셀 academicYear 필드 '학년' 안 붙어 나오는 경우 제외
                academicYear = parsedTag.academicYear.filterNotNull().filter { it.length > 1 }.sorted(),
                classification = parsedTag.classification.filterNotNull().filter { it.isNotBlank() }.sorted(),
                department = parsedTag.department.filterNotNull().filter { it.isNotBlank() }.sorted(),
                credit = parsedTag.credit.sorted().map { "${it}학점" },
                instructor = parsedTag.instructor.filterNotNull().filter { it.isNotBlank() }.sorted(),
                category = parsedTag.category.filterNotNull().filter { it.isNotBlank() }.sorted(),
                sortCriteria = SortCriteria.values().map { it.fullName }.sorted()
            )
        }
        val tagList = tagListRepository.findByYearAndSemester(coursebook.year, coursebook.semester)
            ?.copy(tagCollection = tagCollection, updatedAt = Instant.now()) ?: TagList(
            year = coursebook.year,
            semester = coursebook.semester,
            tagCollection = tagCollection
        )
        tagListRepository.save(tagList)
    }

    private suspend fun syncLectures(compareResult: SugangSnuLectureCompareResult) {
        val updatedLectures = compareResult.updatedLectureList.map { diff ->
            diff.newData.apply { id = diff.oldData.id }
        }

        lectureService.upsertLectures(compareResult.createdLectureList)
        lectureService.upsertLectures(updatedLectures)
        lectureService.deleteLectures(compareResult.deletedLectureList)
    }

    private suspend fun syncSavedUserLectures(compareResult: SugangSnuLectureCompareResult): List<UserLectureSyncResult> =
        merge(
            syncTimetableLectures(compareResult),
            syncBookmarks(compareResult),
        ).toList()

    private fun syncTimetableLectures(compareResult: SugangSnuLectureCompareResult) =
        merge(
            compareResult.updatedLectureList.map { checkOverlapAndUpdateTimetableLectures(it) }.merge(),
            compareResult.deletedLectureList.map { deleteTimetableLectures(it) }.merge(),
        )

    private fun syncBookmarks(compareResult: SugangSnuLectureCompareResult) =
        merge(
            compareResult.updatedLectureList.map { updateBookmarkLectures(it) }.merge(),
            compareResult.deletedLectureList.map { deleteBookmarkLectures(it) }.merge(),
        )

    private fun updateBookmarkLectures(updatedLecture: UpdatedLecture) =
        bookmarkRepository.findAllContainsLectureId(
            updatedLecture.oldData.year,
            updatedLecture.oldData.semester,
            updatedLecture.oldData.id!!
        ).map { bookmark ->
            bookmark.apply {
                lectures.find { it.id == updatedLecture.oldData.id }?.apply {
                    academicYear = updatedLecture.newData.academicYear
                    category = updatedLecture.newData.category
                    classPlaceAndTimes = updatedLecture.newData.classPlaceAndTimes
                    classification = updatedLecture.newData.classification
                    credit = updatedLecture.newData.credit
                    department = updatedLecture.newData.department
                    instructor = updatedLecture.newData.instructor
                    quota = updatedLecture.newData.quota
                    freshmanQuota = updatedLecture.newData.freshmanQuota
                    remark = updatedLecture.newData.remark
                    lectureNumber = updatedLecture.newData.lectureNumber
                    courseNumber = updatedLecture.newData.courseNumber
                    courseTitle = updatedLecture.newData.courseTitle
                }
            }
        }.let {
            bookmarkRepository.saveAll(it)
        }.map { bookmark ->
            BookmarkLectureUpdateResult(
                bookmark.year,
                bookmark.semester,
                updatedLecture.oldData.courseTitle,
                bookmark.userId,
                updatedLecture.oldData.id!!,
                updatedLecture.updatedField
            )
        }

    private fun checkOverlapAndUpdateTimetableLectures(updatedLecture: UpdatedLecture): Flow<UserLectureSyncResult> =
        timeTableRepository.findAllContainsLectureId(
            updatedLecture.oldData.year,
            updatedLecture.oldData.semester,
            updatedLecture.oldData.id!!
        ).let { timetables ->
            merge(
                updateTimetableLectures(
                    timetables.filter { !isUpdatedTimetableLectureOverlapped(it, updatedLecture) },
                    updatedLecture
                ),
                dropOverlappingLectures(
                    timetables.filter { isUpdatedTimetableLectureOverlapped(it, updatedLecture) },
                    updatedLecture
                )
            )
        }

    private fun updateTimetableLectures(
        timetables: Flow<Timetable>,
        updatedLecture: UpdatedLecture
    ): Flow<TimetableLectureUpdateResult> =
        timetables.map { timetable ->
            timetable.apply {
                lectures.find { it.lectureId == updatedLecture.oldData.id }?.apply {
                    academicYear = updatedLecture.newData.academicYear
                    category = updatedLecture.newData.category
                    classPlaceAndTimes = updatedLecture.newData.classPlaceAndTimes
                    classification = updatedLecture.newData.classification
                    credit = updatedLecture.newData.credit
                    department = updatedLecture.newData.department
                    instructor = updatedLecture.newData.instructor
                    lectureNumber = updatedLecture.newData.lectureNumber
                    quota = updatedLecture.newData.quota
                    freshmanQuota = updatedLecture.newData.freshmanQuota
                    remark = updatedLecture.newData.remark
                    courseNumber = updatedLecture.newData.courseNumber
                    courseTitle = updatedLecture.newData.courseTitle
                }
                updatedAt = Instant.now()
            }
        }.let {
            timeTableRepository.saveAll(it)
        }.map { timetable ->
            TimetableLectureUpdateResult(
                year = timetable.year,
                semester = timetable.semester,
                lectureId = updatedLecture.oldData.id!!,
                userId = timetable.userId,
                timetableTitle = timetable.title,
                timetableId = timetable.id!!,
                courseTitle = updatedLecture.oldData.courseTitle,
                updatedFields = updatedLecture.updatedField
            )
        }

    fun dropOverlappingLectures(
        timetables: Flow<Timetable>,
        updatedLecture: UpdatedLecture
    ) = timetables.map { timetable ->
        timeTableRepository.pullTimetableLectureByLectureId(timetable.id!!, updatedLecture.oldData.id!!)
        TimetableLectureDeleteByOverlapResult(
            year = timetable.year,
            semester = timetable.semester,
            lectureId = updatedLecture.oldData.id!!,
            userId = timetable.userId,
            timetableTitle = timetable.title,
            courseTitle = updatedLecture.oldData.courseTitle,
        )
    }

    fun isUpdatedTimetableLectureOverlapped(timetable: Timetable, updatedLecture: UpdatedLecture) =
        updatedLecture.updatedField.contains(Lecture::classPlaceAndTimes) &&
            timetable.lectures.any {
                it.lectureId != updatedLecture.oldData.id &&
                    ClassTimeUtils.timesOverlap(it.classPlaceAndTimes, updatedLecture.newData.classPlaceAndTimes)
            }

    private fun deleteBookmarkLectures(deletedLecture: Lecture): Flow<BookmarkLectureDeleteResult> =
        bookmarkRepository.findAllContainsLectureId(
            deletedLecture.year, deletedLecture.semester, deletedLecture.id!!
        ).map { bookmark ->
            bookmarkRepository.pullLecture(bookmark.id!!, deletedLecture.id!!)
            BookmarkLectureDeleteResult(
                year = bookmark.year,
                semester = bookmark.semester,
                courseTitle = deletedLecture.courseTitle,
                userId = bookmark.userId,
                lectureId = deletedLecture.id!!,
            )
        }

    private fun deleteTimetableLectures(deletedLecture: Lecture): Flow<TimetableLectureDeleteResult> =
        timeTableRepository.findAllContainsLectureId(
            deletedLecture.year, deletedLecture.semester, deletedLecture.id!!
        ).map { timetable ->
            timeTableRepository.pullTimetableLectureByLectureId(timetable.id!!, deletedLecture.id!!)
            TimetableLectureDeleteResult(
                timetable.year,
                timetable.semester,
                timetable.title,
                deletedLecture.courseTitle,
                timetable.userId,
                deletedLecture.id!!,
            )
        }

    private suspend fun updateLectureBuildings(compareResult: SugangSnuLectureCompareResult) {
        val updatedPlaceInfos = (compareResult.updatedLectureList.map { it.newData } + compareResult.createdLectureList)
            .flatMap { it.classPlaceAndTimes }
            .flatMap { PlaceInfo.getValuesOf(it.place) }
            .filter { it.campus == Campus.GWANAK }
            .distinct()
        lectureBuildingService.updateLectureBuildings(updatedPlaceInfos)
    }

    private fun Coursebook.isSyncedToSugangSnu(sugangSnuCoursebookCondition: SugangSnuCoursebookCondition): Boolean =
        this.year == sugangSnuCoursebookCondition.latestYear && this.semester == sugangSnuCoursebookCondition.latestSemester
}

data class ParsedTags(
    val classification: Set<String?> = setOf(),
    val department: Set<String?> = setOf(),
    val academicYear: Set<String?> = setOf(),
    val credit: Set<Long> = setOf(),
    val instructor: Set<String?> = setOf(),
    val category: Set<String?> = setOf(),
    val etc: Set<String?> = setOf(),
)
