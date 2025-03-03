package com.wafflestudio.snutt.notification.service

import com.wafflestudio.snutt.common.push.dto.PushMessage
import com.wafflestudio.snutt.notification.data.NotificationType
import com.wafflestudio.snutt.notification.data.PushCategory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

interface PushWithNotificationService {
    /**
     * 한 명의 유저에게 푸시를 보내고 알림함에 보이게 합니다.
     */
    suspend fun sendPushAndNotification(
        pushMessage: PushMessage,
        notificationType: NotificationType,
        userId: String,
    )

    /**
     * 복수 명의 유저에게 푸시를 보내고 알림함에 보이게 합니다.
     */
    suspend fun sendPushesAndNotifications(
        pushMessage: PushMessage,
        notificationType: NotificationType,
        userIds: List<String>,
    )

    /**
     * 모든 유저에게 푸시를 보내고 해당 내용이 알림함에 보이게 합니다.
     */
    suspend fun sendGlobalPushAndNotification(
        pushMessage: PushMessage,
        notificationType: NotificationType,
    )
}

@Service
class PushWithNotificationServiceImpl internal constructor(
    private val pushService: PushService,
    private val notificationService: NotificationService,
) : PushWithNotificationService {
    override suspend fun sendPushAndNotification(
        pushMessage: PushMessage,
        notificationType: NotificationType,
        userId: String,
    ): Unit =
        coroutineScope {
            launch { notificationService.sendNotification(pushMessage.toNotification(notificationType, userId)) }
            launch { pushService.sendCategoricalPush(pushMessage, userId, PushCategory(notificationType)) }
        }

    override suspend fun sendPushesAndNotifications(
        pushMessage: PushMessage,
        notificationType: NotificationType,
        userIds: List<String>,
    ): Unit =
        coroutineScope {
            launch {
                notificationService.sendNotifications(
                    userIds.map {
                        pushMessage.toNotification(
                            notificationType,
                            it,
                        )
                    },
                )
            }
            launch { pushService.sendCategoricalPushes(pushMessage, userIds, PushCategory(notificationType)) }
        }

    override suspend fun sendGlobalPushAndNotification(
        pushMessage: PushMessage,
        notificationType: NotificationType,
    ): Unit =
        coroutineScope {
            launch { notificationService.sendNotification(pushMessage.toNotification(notificationType, userId = null)) }
            launch { pushService.sendGlobalPush(pushMessage) }
        }
}
