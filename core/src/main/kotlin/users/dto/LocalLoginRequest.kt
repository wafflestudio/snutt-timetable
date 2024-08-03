package com.wafflestudio.snu4t.users.dto

import com.fasterxml.jackson.annotation.JsonAlias

data class LocalLoginRequest(
    @JsonAlias("user_id")
    val id: String,
    val password: String,
)
