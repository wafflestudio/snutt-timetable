package com.wafflestudio.snutt.notification.service

import com.wafflestudio.snutt.common.push.PushClient
import com.wafflestudio.snutt.common.push.dto.PushMessage
import com.wafflestudio.snutt.common.push.dto.TargetedPushMessageWithToken
import com.wafflestudio.snutt.notification.data.PushCategory
import com.wafflestudio.snutt.notification.repository.PushOptOutRepository
import org.springframework.stereotype.Service

/**
 * 유저 기기에 푸시를 보내는 서비스입니다.
 */
interface PushService {
    suspend fun sendPush(
        pushMessage: PushMessage,
        userId: String,
    )

    suspend fun sendPushes(
        pushMessage: PushMessage,
        userIds: List<String>,
    )

    suspend fun sendGlobalPush(pushMessage: PushMessage)

    suspend fun sendTargetPushes(userToPushMessage: Map<String, PushMessage>)

    suspend fun sendCategoricalPush(
        pushMessage: PushMessage,
        userId: String,
        pushCategory: PushCategory,
    )

    suspend fun sendCategoricalPushes(
        pushMessage: PushMessage,
        userIds: List<String>,
        pushCategory: PushCategory,
    )

    suspend fun sendCategoricalTargetPushes(
        userToPushMessage: Map<String, PushMessage>,
        pushCategory: PushCategory,
    )
}

@Service
class PushServiceImpl internal constructor(
    private val deviceService: DeviceService,
    private val pushClient: PushClient,
    private val pushOptOutRepository: PushOptOutRepository,
) : PushService {
    override suspend fun sendPush(
        pushMessage: PushMessage,
        userId: String,
    ) {
        val userDevices = deviceService.getUserDevices(userId).ifEmpty { return }

        val targetedPushMessageWithTokens = userDevices.map { TargetedPushMessageWithToken(it.fcmRegistrationId, pushMessage) }
        pushClient.sendMessages(targetedPushMessageWithTokens)
    }

    override suspend fun sendPushes(
        pushMessage: PushMessage,
        userIds: List<String>,
    ) {
        val userIdToDevices = deviceService.getUsersDevices(userIds).ifEmpty { return }

        val targetedPushMessageWithTokens =
            userIdToDevices.values.flatMap { userDevices ->
                userDevices.map { TargetedPushMessageWithToken(it.fcmRegistrationId, pushMessage) }
            }

        pushClient.sendMessages(targetedPushMessageWithTokens)
    }

    override suspend fun sendGlobalPush(pushMessage: PushMessage) = pushClient.sendGlobalMessage(pushMessage)

    override suspend fun sendTargetPushes(userToPushMessage: Map<String, PushMessage>) =
        userToPushMessage.entries.flatMap { (userId, pushMessage) ->
            deviceService.getUserDevices(userId).map { it.fcmRegistrationId to pushMessage }
        }.map { (fcmRegistrationId, message) -> TargetedPushMessageWithToken(fcmRegistrationId, message) }
            .let { pushClient.sendMessages(it) }

    override suspend fun sendCategoricalPush(
        pushMessage: PushMessage,
        userId: String,
        pushCategory: PushCategory,
    ) {
        if (pushCategory == PushCategory.NORMAL || !pushOptOutRepository.existsByUserIdAndPushCategory(userId, pushCategory)) {
            sendPush(pushMessage, userId)
        }
    }

    override suspend fun sendCategoricalPushes(
        pushMessage: PushMessage,
        userIds: List<String>,
        pushCategory: PushCategory,
    ) {
        if (pushCategory == PushCategory.NORMAL) {
            sendPushes(pushMessage, userIds)
        }

        val filteredUserIds =
            pushOptOutRepository
                .findByUserIdInAndPushCategory(userIds, pushCategory)
                .map { it.userId }
                .toSet()
                .let { optOutUserIds -> userIds.filterNot { it in optOutUserIds } }

        if (filteredUserIds.isNotEmpty()) {
            sendPushes(pushMessage, filteredUserIds)
        }
    }

    override suspend fun sendCategoricalTargetPushes(
        userToPushMessage: Map<String, PushMessage>,
        pushCategory: PushCategory,
    ) {
        if (pushCategory == PushCategory.NORMAL) {
            sendTargetPushes(userToPushMessage)
        }

        val userIds = userToPushMessage.keys.toList()

        val filteredUserToPushMessage =
            pushOptOutRepository
                .findByUserIdInAndPushCategory(userIds, pushCategory)
                .map { it.userId }
                .toSet()
                .let { optOutUserIds -> userToPushMessage.filterKeys { it !in optOutUserIds } }

        if (filteredUserToPushMessage.isNotEmpty()) {
            sendTargetPushes(filteredUserToPushMessage)
        }
    }
}
