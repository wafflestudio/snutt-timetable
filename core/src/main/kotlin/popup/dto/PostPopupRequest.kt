package com.wafflestudio.snu4t.popup.dto

data class PostPopupRequest(
    val key: String,
    val imageOriginUri: String,
    // null 이면 '당분간 보지 않기' 눌러도 매번 노출
    val hiddenDays: Int?,
)
