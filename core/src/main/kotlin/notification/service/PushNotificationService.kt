package com.wafflestudio.snu4t.notification.service

import com.wafflestudio.snu4t.common.push.PushClient
import com.wafflestudio.snu4t.common.push.dto.PushMessage
import com.wafflestudio.snu4t.common.push.dto.PushTargetMessage
import com.wafflestudio.snu4t.notification.data.Notification
import com.wafflestudio.snu4t.notification.data.NotificationType
import com.wafflestudio.snu4t.notification.dto.NotificationQuery
import com.wafflestudio.snu4t.notification.repository.NotificationRepository
import com.wafflestudio.snu4t.notification.repository.countUnreadNotifications
import com.wafflestudio.snu4t.users.data.User
import com.wafflestudio.snu4t.users.service.UserService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.time.LocalDateTime

interface PushNotificationService : PushService, NotificationService {
    /**
     * 한 명의 유저에게 푸시를 보내고 알림함에 보이게 합니다.
     */
    suspend fun sendPushAndNotification(pushMessage: PushMessage, notificationType: NotificationType, userId: String)

    /**
     * 복수 명의 유저에게 푸시를 보내고 알림함에 보이게 합니다.
     */
    suspend fun sendPushesAndNotifications(pushMessage: PushMessage, notificationType: NotificationType, userIds: List<String>)

    /**
     * 모든 유저에게 푸시를 보내고 해당 내용이 알림함에 보이게 합니다.
     */
    suspend fun sendGlobalPushAndNotification(pushMessage: PushMessage, notificationType: NotificationType)
}

@Service
class PushNotificationServiceImpl internal constructor(
    private val userService: UserService,
    private val deviceService: DeviceService,
    private val pushClient: PushClient,
    private val notificationRepository: NotificationRepository,
) : PushNotificationService {
    override suspend fun sendPushAndNotification(
        pushMessage: PushMessage,
        notificationType: NotificationType,
        userId: String,
    ): Unit = coroutineScope {
        launch { sendNotification(pushMessage.toNotification(notificationType, userId)) }
        launch { sendPush(pushMessage, userId) }
    }

    override suspend fun sendPushesAndNotifications(
        pushMessage: PushMessage,
        notificationType: NotificationType,
        userIds: List<String>,
    ): Unit = coroutineScope {
        launch { sendNotifications(userIds.map { pushMessage.toNotification(notificationType, it) }) }
        launch { sendPushes(pushMessage, userIds) }
    }

    override suspend fun sendGlobalPushAndNotification(
        pushMessage: PushMessage,
        notificationType: NotificationType,
    ): Unit = coroutineScope {
        launch { sendNotification(pushMessage.toNotification(notificationType, userId = null)) }
        launch { pushClient.sendGlobalMessage(pushMessage) }
    }

    override suspend fun getNotifications(query: NotificationQuery): List<Notification> {
        val user = query.user
        val notifications = notificationRepository.findNotifications(
            userId = user.id!!,
            createdAt = user.regDate,
            offset = query.offset,
            limit = query.limit,
        ).toList()

        if (query.explicit) {
            userService.update(user.apply { notificationCheckedAt = LocalDateTime.now() })
        }

        return notifications
    }

    override suspend fun getUnreadCount(user: User): Long {
        return notificationRepository.countUnreadNotifications(user.id!!, user.notificationCheckedAt)
    }

    override suspend fun sendNotification(notification: Notification) {
        notificationRepository.save(notification)
    }

    override suspend fun sendNotifications(notifications: List<Notification>) {
        notificationRepository.saveAll(notifications).toList()
    }

    override suspend fun sendPush(pushMessage: PushMessage, userId: String) {
        val userDevices = deviceService.getUserDevices(userId).ifEmpty { return }

        val pushTargetMessages = userDevices.map { PushTargetMessage(it.fcmRegistrationId, pushMessage) }
        pushClient.sendMessages(pushTargetMessages)
    }

    override suspend fun sendPushes(pushMessage: PushMessage, userIds: List<String>) {
        val userIdToDevices = deviceService.getUsersDevices(userIds).ifEmpty { return }

        val pushTargetMessages = userIdToDevices.values.flatMap { userDevices ->
            userDevices.map { PushTargetMessage(it.fcmRegistrationId, pushMessage) }
        }

        pushClient.sendMessages(pushTargetMessages)
    }
}
