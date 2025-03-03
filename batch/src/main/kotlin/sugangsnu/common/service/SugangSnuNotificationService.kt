package com.wafflestudio.snutt.sugangsnu.common.service

import com.wafflestudio.snutt.common.push.DeeplinkType
import com.wafflestudio.snutt.common.push.dto.PushMessage
import com.wafflestudio.snutt.coursebook.data.Coursebook
import com.wafflestudio.snutt.notification.data.Notification
import com.wafflestudio.snutt.notification.data.NotificationType
import com.wafflestudio.snutt.notification.data.PushCategory
import com.wafflestudio.snutt.notification.service.NotificationService
import com.wafflestudio.snutt.notification.service.PushService
import com.wafflestudio.snutt.notification.service.PushWithNotificationService
import com.wafflestudio.snutt.sugangsnu.common.utils.toKoreanFieldName
import com.wafflestudio.snutt.sugangsnu.job.sync.data.BookmarkLectureDeleteResult
import com.wafflestudio.snutt.sugangsnu.job.sync.data.BookmarkLectureUpdateResult
import com.wafflestudio.snutt.sugangsnu.job.sync.data.TimetableLectureDeleteByOverlapResult
import com.wafflestudio.snutt.sugangsnu.job.sync.data.TimetableLectureDeleteResult
import com.wafflestudio.snutt.sugangsnu.job.sync.data.TimetableLectureSyncResult
import com.wafflestudio.snutt.sugangsnu.job.sync.data.TimetableLectureUpdateResult
import com.wafflestudio.snutt.sugangsnu.job.sync.data.UserLectureSyncResult
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service

interface SugangSnuNotificationService {
    suspend fun notifyUserLectureChanges(userLectureSyncResults: List<UserLectureSyncResult>)

    suspend fun notifyCoursebookUpdate(coursebook: Coursebook)
}

@Service
class SugangSnuNotificationServiceImpl(
    private val pushWithNotificationService: PushWithNotificationService,
    private val notificationService: NotificationService,
    private val pushService: PushService,
) : SugangSnuNotificationService {
    override suspend fun notifyUserLectureChanges(userLectureSyncResults: List<UserLectureSyncResult>): Unit =
        coroutineScope {
            val notifications = userLectureSyncResults.map { it.toNotification() }
            notificationService.sendNotifications(notifications)
            sendPushForTimetable(userLectureSyncResults.filterIsInstance<TimetableLectureSyncResult>())
        }

    private suspend fun sendPushForTimetable(userLectureSyncResults: List<TimetableLectureSyncResult>) =
        coroutineScope {
            val userUpdatedLectureCountMap =
                userLectureSyncResults.filterIsInstance<TimetableLectureUpdateResult>().toCountMap()
            val userDeletedLectureCountMap =
                userLectureSyncResults.filter { it is TimetableLectureDeleteResult || it is TimetableLectureDeleteByOverlapResult }
                    .toCountMap()

            val allUserIds = userUpdatedLectureCountMap.keys + userDeletedLectureCountMap.keys

            val userIdToMessage =
                allUserIds.associateWith { userId ->
                    val updatedCount = userUpdatedLectureCountMap[userId]
                    val deletedCount = userDeletedLectureCountMap[userId]

                    val messageBody =
                        when {
                            updatedCount != null && deletedCount != null -> {
                                "강의 ${updatedCount}개가 변경, ${deletedCount}개가 삭제되었습니다. 알림함에서 자세히 확인하세요."
                            }
                            updatedCount != null -> {
                                "강의 ${updatedCount}개가 변경되었습니다. 알림함에서 자세히 확인하세요."
                            }
                            deletedCount != null -> {
                                "강의 ${deletedCount}개가 삭제되었습니다. 알림함에서 자세히 확인하세요."
                            }
                            else -> {
                                error("This should not happen")
                            }
                        }
                    PushMessage(
                        title = "수강편람 업데이트",
                        body = messageBody,
                        urlScheme = DeeplinkType.NOTIFICATIONS,
                    )
                }
            pushService.sendCategoricalTargetPushes(userIdToMessage, PushCategory.LECTURE_UPDATE)
        }

    override suspend fun notifyCoursebookUpdate(coursebook: Coursebook) {
        val messageBody = "${coursebook.year}년도 ${coursebook.semester.fullName} 수강편람이 추가되었습니다."

        pushWithNotificationService.sendGlobalPushAndNotification(
            PushMessage(title = "신규 수강편람", body = messageBody),
            NotificationType.COURSEBOOK,
        )
    }

    private fun List<UserLectureSyncResult>.toCountMap() =
        this.map { result -> result.userId to result.lectureId }.distinct().groupingBy { it.first }.eachCount()

    private fun UserLectureSyncResult.toNotification(): Notification {
        val (message, notificationType, deeplink) =
            when (this) {
                // 업데이트 알림
                is TimetableLectureUpdateResult -> {
                    Triple(
                        """
                        $year-${semester.fullName} '$timetableTitle' 시간표의 
                        '$courseTitle' 강의가 업데이트 되었습니다.
                        (항목: ${updatedFields.map { field -> field.toKoreanFieldName() }.distinct().joinToString()})
                        """.trimIndent().replace("\n", ""),
                        NotificationType.LECTURE_UPDATE,
                        DeeplinkType.TIMETABLE_LECTURE.build(timetableId, lectureId),
                    )
                }
                is BookmarkLectureUpdateResult -> {
                    Triple(
                        """
                        $year-${semester.fullName} 관심강좌 목록의 '$courseTitle' 강의가 업데이트 되었습니다.
                        (항목: ${updatedFields.map { field -> field.toKoreanFieldName() }.distinct().joinToString()})
                        """.trimIndent().replace("\n", ""),
                        NotificationType.LECTURE_UPDATE,
                        DeeplinkType.BOOKMARKS.build(year, semester, lectureId),
                    )
                }
                // 폐강 알림
                is TimetableLectureDeleteResult -> {
                    Triple(
                        """
                        $year-${semester.fullName} '$timetableTitle' 시간표의 
                        '$courseTitle' 강의가 폐강되어 삭제되었습니다.
                        """.trimIndent().replace("\n", ""),
                        NotificationType.LECTURE_REMOVE,
                        DeeplinkType.NOTIFICATIONS.build(),
                    )
                }
                is BookmarkLectureDeleteResult -> {
                    Triple(
                        """
                        $year-${semester.fullName} 관심강좌 목록의 
                        '$courseTitle' 강의가 폐강되어 삭제되었습니다.
                        """.trimIndent().replace("\n", ""),
                        NotificationType.LECTURE_REMOVE,
                        DeeplinkType.NOTIFICATIONS.build(),
                    )
                }
                is TimetableLectureDeleteByOverlapResult -> {
                    Triple(
                        """
                        $year-${semester.fullName} '$timetableTitle' 시간표의 
                        '$courseTitle' 강의가 업데이트되었으나, 시간표의 다른 강의와 겹쳐 삭제되었습니다.
                        """.trimIndent().replace("\n", ""),
                        NotificationType.LECTURE_REMOVE,
                        DeeplinkType.NOTIFICATIONS.build(),
                    )
                }
            }

        return Notification(
            userId = userId,
            title = "수강편람 업데이트",
            message = message,
            type = notificationType,
            deeplink = deeplink.value,
        )
    }
}
