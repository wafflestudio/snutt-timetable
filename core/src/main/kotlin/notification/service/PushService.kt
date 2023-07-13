package com.wafflestudio.snu4t.notification.service

import com.wafflestudio.snu4t.common.push.PushClient
import com.wafflestudio.snu4t.common.push.dto.PushMessage
import com.wafflestudio.snu4t.common.push.dto.PushTargetMessage
import org.springframework.stereotype.Service

/**
 * 유저 기기에 푸시를 보내는 서비스입니다.
 */
interface PushService {
    suspend fun sendPush(pushMessage: PushMessage, userId: String)
    suspend fun sendPushes(pushMessage: PushMessage, userIds: List<String>)
    suspend fun sendGlobalPush(pushMessage: PushMessage)
    suspend fun sendTargetPushes(userToPushMessage: Map<String, PushMessage>)
}

@Service
class PushServiceImpl internal constructor(
    private val deviceService: DeviceService,
    private val pushClient: PushClient,
) : PushService {
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

    override suspend fun sendGlobalPush(pushMessage: PushMessage) = pushClient.sendGlobalMessage(pushMessage)

    override suspend fun sendTargetPushes(userToPushMessage: Map<String, PushMessage>) =
        userToPushMessage.entries.flatMap { (userId, pushMessage) ->
            deviceService.getUserDevices(userId).map { it.fcmRegistrationId to pushMessage }
        }.map { (fcmRegistrationId, message) -> PushTargetMessage(fcmRegistrationId, message) }
            .let { pushClient.sendMessages(it) }
}
