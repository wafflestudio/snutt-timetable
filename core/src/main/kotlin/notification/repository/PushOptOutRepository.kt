package com.wafflestudio.snutt.notification.repository

import com.wafflestudio.snutt.notification.data.PushCategory
import com.wafflestudio.snutt.notification.data.PushOptOut
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PushOptOutRepository : CoroutineCrudRepository<PushOptOut, String> {
    suspend fun existsByUserIdAndPushCategory(
        userId: String,
        pushCategory: PushCategory,
    ): Boolean

    suspend fun deleteByUserIdAndPushCategory(
        userId: String,
        pushCategory: PushCategory,
    ): Long

    suspend fun findByUserIdInAndPushCategory(
        userIds: List<String>,
        pushCategory: PushCategory,
    ): List<PushOptOut>

    suspend fun findByUserId(userId: String): List<PushOptOut>
}
