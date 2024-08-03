package com.wafflestudio.snu4t.users.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetMaskedEmailRequest(
    @JsonProperty("user_id")
    val userId: String,
)
