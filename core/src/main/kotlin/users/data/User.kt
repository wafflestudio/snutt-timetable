package com.wafflestudio.snu4t.users.data

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("users")
data class User(
    @Id
    val id: String? = null,
    var email: String?,
    var isEmailVerified: Boolean?,
    var credential: Credential,
    var credentialHash: String,
    var fcmKey: String?,
    var active: Boolean = true,
    var isAdmin: Boolean = false,
    val regDate: LocalDateTime = LocalDateTime.now(),
    var lastLoginTimestamp: Long = System.currentTimeMillis(),
    var notificationCheckedAt: LocalDateTime = LocalDateTime.now(),
)
