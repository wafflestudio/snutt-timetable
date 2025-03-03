package com.wafflestudio.snutt.notification.dto

import com.wafflestudio.snutt.notification.data.PushCategory

data class PushPreference(
    val pushCategory: PushCategory,
    val enabled: Boolean,
)
