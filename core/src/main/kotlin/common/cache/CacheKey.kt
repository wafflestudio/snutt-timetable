package com.wafflestudio.snu4t.common.cache

import java.time.Duration

interface BuiltCacheKey {
    val key: String
    val ttl: Duration
}

enum class CacheKey(
    private val keyFormat: String,
    private val ttl: Duration,
) {
    CLIENT_CONFIGS("client_configs:%s_%s", Duration.ofMinutes(5)), // osType, appVersion
    LOCK_REGISTER_LOCAL("lock:register_local:%s", Duration.ofMinutes(1)), // localId
    LOCK_ADD_FCM_REGISTRATION_ID("lock:add_registration_id:%s_%s", Duration.ofMinutes(1)), // userId, registrationId
    LOCK_LIVE_SUGANG_SNU_SYNC_UNTIL_CONFIRMED("lock:live_sugang_snu_sync_until_confirmed", Duration.ofDays(14)),
    ;

    fun build(vararg args: Any?): BuiltCacheKey {
        val key = keyFormat.format(*args)
        return object : BuiltCacheKey {
            override val key: String = key
            override val ttl: Duration = this@CacheKey.ttl
        }
    }
}
