package com.wafflestudio.snu4t.users.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class EmailVerificationResultDto(
    @JsonProperty("is_email_verified")
    val isEmailVerified: Boolean = false
)
