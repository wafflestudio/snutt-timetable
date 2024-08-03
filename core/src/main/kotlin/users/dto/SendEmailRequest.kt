package com.wafflestudio.snu4t.users.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class SendEmailRequest(
    @JsonProperty("email")
    @JsonAlias("user_email")
    val email: String
)
