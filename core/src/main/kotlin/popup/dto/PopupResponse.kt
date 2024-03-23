package com.wafflestudio.snu4t.popup.dto

import com.wafflestudio.snu4t.popup.data.Popup

data class PopupResponse(
    val id: String,
    val key: String,
    val imageUri: String,
    @Deprecated("Use imageUri instead", replaceWith = ReplaceWith("imageUri"))
    val image_url: String,
    val hiddenDays: Int?,
    @Deprecated("Use hiddenDays instead", replaceWith = ReplaceWith("hiddenDays"))
    val hidden_days: Int?,
)

fun PopupResponse(popup: Popup): PopupResponse = PopupResponse(
    id = popup.id!!,
    key = popup.key,
    imageUri = popup.imageOriginUri,
    image_url = popup.imageOriginUri,
    hiddenDays = popup.hiddenDays,
    hidden_days = popup.hiddenDays,
)
