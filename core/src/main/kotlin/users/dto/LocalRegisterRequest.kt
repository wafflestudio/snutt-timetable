package com.wafflestudio.snu4t.users.dto

data class LocalRegisterRequest(
    val id: String,
    val password: String,
    val email: String?,
)
