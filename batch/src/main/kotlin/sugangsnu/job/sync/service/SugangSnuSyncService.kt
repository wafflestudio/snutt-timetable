package com.wafflestudio.snu4t.sugangsnu.job.sync.service

import com.wafflestudio.snu4t.bookmark.repository.BookmarkRepository
import com.wafflestudio.snu4t.common.cache.Cache
import com.wafflestudio.snu4t.common.cache.CacheKey
import com.wafflestudio.snu4t.common.cache.get
import com.wafflestudio.snu4t.common.slack.CONFIRM_DONE_EMOJI
import com.wafflestudio.snu4t.common.slack.CONFIRM_ONGOING_EMOJI
import com.wafflestudio.snu4t.common.slack.SlackMessageBlock
import com.wafflestudio.snu4t.common.slack.SlackMessageRequest
import com.wafflestudio.snu4t.common.slack.SlackMessageService
import com.wafflestudio.snu4t.config.Phase
import com.wafflestudio.snu4t.config.SNUTT_MENTION
import com.wafflestudio.snu4t.coursebook.data.Coursebook
import com.wafflestudio.snu4t.coursebook.repository.CoursebookRepository
import com.wafflestudio.snu4t.lecturebuildings.data.PlaceInfo
import com.wafflestudio.snu4t.lecturebuildings.repository.LectureBuildingRepository
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
    private val lectureBuildingRepository: LectureBuildingRepository,
    private val cache: Cache,
    private val phase: Phase,
    private val slackMessageService: SlackMessageService
) : SugangSnuSyncService {

    override suspend fun updateCoursebook(coursebook: Coursebook): List<UserLectureSyncResult> {
        val newLectures = sugangSnuFetchService.getSugangSnuLectures(coursebook.year, coursebook.semester)
        val oldLectures =
            lectureService.getLecturesByYearAndSemesterAsFlow(coursebook.year, coursebook.semester).toList()
        var compareResult = compareLectures(newLectures, oldLectures)
        val compareResultWithBuildingInfo = addBuildingInfos(compareResult)

        syncLectures(compareResultWithBuildingInfo)
        val syncUserLecturesResults = syncSavedUserLectures(compareResultWithBuildingInfo)
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

        val ongoingConfirmThread = cache.get<String>(CacheKey.LOCK_LIVE_SUGANG_SNU_SYNC_UNTIL_CONFIRMED.build())
        val hasOngoingConfirmProcessOnProd = phase.isProd && ongoingConfirmThread != null
        val currentResultRequiresConfirm = phase.isProd && compareResult.needsConfirmOnProduction()

        when {
            currentResultRequiresConfirm -> {
                // 이전 검토 요청 존재 여부와 별개로 새로운 검토 요청
                val newThreadTs = slackMessageService.postMessage(
                    message =
                    SlackMessageRequest(
                        SlackMessageBlock.Section("*$SNUTT_MENTION 수강스누 업데이트 검토가 필요합니다.*"),
                        SlackMessageBlock.Section("추가된 강좌 수: ${compareResult.createdLectureList.size}"),
                        SlackMessageBlock.Section("업데이트된 강좌 수: ${compareResult.updatedLectureList.size}"),
                        SlackMessageBlock.Section("삭제된 강좌 수: ${compareResult.deletedLectureList.size}"),
                        SlackMessageBlock.Section("개발환경에서 검토 후 아래 버튼 통해 라이브 배포해주세요."),
                        SlackMessageBlock.Action.SUGANG_SNU_CONFIRM
                    )
                ).threadTs

                // 기존 결과가 검토 대기 중이었으면 내림
                if (hasOngoingConfirmProcessOnProd) {
                    slackMessageService.deleteEmoji(threadTs = ongoingConfirmThread!!, emoji = CONFIRM_ONGOING_EMOJI)
                    slackMessageService.addEmoji(threadTs = ongoingConfirmThread, emoji = CONFIRM_DONE_EMOJI)
                    slackMessageService.postMessageToThread(
                        threadTs = ongoingConfirmThread,
                        message = SlackMessageRequest(
                            SlackMessageBlock.Header("새로운 동기화 결과가 업데이트 되어 검토 요청을 자동 종료합니다."),
                            SlackMessageBlock.Section("새로운 동기화 결과가 자동 배포 기준을 충족하지 않아 검토가 필요합니다."),
                        )
                    )
                }

                slackMessageService.addEmoji(threadTs = newThreadTs, emoji = CONFIRM_ONGOING_EMOJI)
                cache.set(CacheKey.LOCK_LIVE_SUGANG_SNU_SYNC_UNTIL_CONFIRMED.build(), newThreadTs)
            }

            // 이전 검토 요청이 있었고, 새로운 결과가 검토 대기 중이 아니면 검토 종료, 새로운 결과 반영
            !currentResultRequiresConfirm && hasOngoingConfirmProcessOnProd -> {
                slackMessageService.deleteEmoji(threadTs = ongoingConfirmThread!!, emoji = CONFIRM_ONGOING_EMOJI)
                slackMessageService.addEmoji(threadTs = ongoingConfirmThread, emoji = CONFIRM_DONE_EMOJI)
                slackMessageService.postMessageToThread(
                    threadTs = ongoingConfirmThread,
                    message = SlackMessageRequest(
                        SlackMessageBlock.Header("동기화 결과가 업데이트 되어 검토 요청을 자동 종료합니다."),
                        SlackMessageBlock.Section("새로운 동기화 결과가 자동 배포 기준을 충족하여 검토가 필요하지 않습니다."),
                        SlackMessageBlock.Section("검토 요청을 종료합니다."),
                    )
                )

                lectureService.upsertLectures(compareResult.createdLectureList)
                lectureService.upsertLectures(updatedLectures)
                lectureService.deleteLectures(compareResult.deletedLectureList)
            }

            else -> {
                lectureService.upsertLectures(compareResult.createdLectureList)
                lectureService.upsertLectures(updatedLectures)
                lectureService.deleteLectures(compareResult.deletedLectureList)
            }
        }
    }

    private suspend fun addBuildingInfos(compareResult: SugangSnuLectureCompareResult): SugangSnuLectureCompareResult {
        val createdLectureListWithBuildingInfo = populateLecturesWithBuildingInfo(compareResult.createdLectureList)

        val updatedLectures = compareResult.updatedLectureList.map { it.newData }
        val updatedLecturesWithBuildingInfo = populateLecturesWithBuildingInfo(updatedLectures)
            .associateBy { it.id }
        val updatedLectureList = compareResult.updatedLectureList.map {
            it.apply {
                this.newData = this.newData.id?.let { updatedLecturesWithBuildingInfo[it] } ?: this.newData
            }
        }

        return SugangSnuLectureCompareResult(
            createdLectureList = createdLectureListWithBuildingInfo,
            deletedLectureList = compareResult.deletedLectureList,
            updatedLectureList = updatedLectureList
        )
    }

    // 미리 저장해둔 빌딩 정보로 강의에 lectureBuilding 정보 추가
    private suspend fun populateLecturesWithBuildingInfo(lectures: List<Lecture>): List<Lecture> {
        val buildingNumbers = lectures
            .flatMap { it.classPlaceAndTimes }
            .map { PlaceInfo(it.place)?.buildingNumber }
            .filterNotNull()

        val buildingsMap = lectureBuildingRepository.findByBuildingNumberIsIn(buildingNumbers.toSet())
            .associateBy { it.buildingNumber }

        return lectures.map { lecture ->
            lecture.apply {
                this.classPlaceAndTimes = lecture.classPlaceAndTimes.map { classPlaceAndTime ->
                    classPlaceAndTime.apply {
                        this.lectureBuildings = PlaceInfo.getValuesOf(place)
                            .mapNotNull { buildingsMap[it.buildingNumber] }
                    }
                }
            }
        }
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
                    ClassTimeUtils.timesOverlap(
                        it.classPlaceAndTimes,
                        updatedLecture.newData.classPlaceAndTimes
                    )
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
