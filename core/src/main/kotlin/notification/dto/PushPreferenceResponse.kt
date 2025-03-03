package com.wafflestudio.snutt.notification.dto

data class PushPreferenceResponse(
    val pushCategoryName: String,
    val enabled: Boolean,
)

fun PushPreferenceResponse(pushPreference: PushPreference) =
    PushPreferenceResponse(
        pushCategoryName = pushPreference.pushCategory.name,
        enabled = pushPreference.enabled,
    )
