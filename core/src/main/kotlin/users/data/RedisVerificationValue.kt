package com.wafflestudio.snu4t.users.data

import java.time.LocalDateTime

data class RedisVerificationValue(
    val email: String,
    val code: String,
    val count: Int = 1,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
