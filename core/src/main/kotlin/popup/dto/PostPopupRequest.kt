package com.wafflestudio.snu4t.popup.dto

data class PostPopupRequest(
    val key: String,
    val imageOriginUri: String,
    val hiddenDays: Int?,
)
