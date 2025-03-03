package com.wafflestudio.snutt.notification.service

import com.wafflestudio.snutt.notification.data.PushCategory
import com.wafflestudio.snutt.notification.data.PushOptOut
import com.wafflestudio.snutt.notification.dto.PushPreference
import com.wafflestudio.snutt.notification.repository.PushOptOutRepository
import com.wafflestudio.snutt.users.data.User
import org.springframework.stereotype.Service

interface PushPreferenceService {
    suspend fun enablePush(
        user: User,
        pushCategory: PushCategory,
    )

    suspend fun disablePush(
        user: User,
        pushCategory: PushCategory,
    )

    suspend fun getPushPreferences(user: User): List<PushPreference>
}

@Service
class PushPreferenceServiceImpl(
    private val pushOptOutRepository: PushOptOutRepository,
) : PushPreferenceService {
    override suspend fun enablePush(
        user: User,
        pushCategory: PushCategory,
    ) {
        pushOptOutRepository.save(
            PushOptOut(
                userId = user.id!!,
                pushCategory = pushCategory,
            ),
        )
    }

    override suspend fun disablePush(
        user: User,
        pushCategory: PushCategory,
    ) {
        pushOptOutRepository.deleteByUserIdAndPushCategory(
            userId = user.id!!,
            pushCategory = pushCategory,
        )
    }

    override suspend fun getPushPreferences(user: User): List<PushPreference> {
        val allPushCategories = PushCategory.entries.filterNot { it == PushCategory.NORMAL }
        val disabledPushCategories = pushOptOutRepository.findByUserId(user.id!!).map { it.pushCategory }.toSet()
        return allPushCategories.map {
            if (it in disabledPushCategories) {
                return@map PushPreference(
                    pushCategory = it,
                    enabled = false,
                )
            } else {
                return@map PushPreference(
                    pushCategory = it,
                    enabled = true,
                )
            }
        }
    }
}
