package com.wafflestudio.snutt.auth

enum class AuthProvider(val value: String) {
    LOCAL("local"),
    FACEBOOK("facebook"),
    APPLE("apple"),
    GOOGLE("google"),
    KAKAO("kakao"),
    ;

    companion object {
        private val mapping = entries.associateBy { e -> e.value }

        fun from(value: String): AuthProvider? = mapping[value]
    }
}
