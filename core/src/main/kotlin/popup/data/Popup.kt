package com.wafflestudio.snu4t.popup.data

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

@Document
data class Popup(
    @Id
    val id: String? = null,
    @Field
    val key: String,
    @Field
    val imageOriginUri: String,
    @Field
    val hiddenDays: Int?, // null 이면 '당분간 보지 않기' 눌러도 매번 노출
    @Field
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Field
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
