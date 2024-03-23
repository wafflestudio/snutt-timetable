package com.wafflestudio.snu4t.clientconfig.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wafflestudio.snu4t.clientconfig.data.ClientConfig
import com.wafflestudio.snu4t.common.client.AppVersion

data class PostConfigRequest(
    val data: JsonNode,
    val minVersion: ConfigVersionDto?,
    val maxVersion: ConfigVersionDto?,
)

data class PatchConfigRequest(
    val data: JsonNode?,
    val minVersion: ConfigVersionDto?,
    val maxVersion: ConfigVersionDto?,
)

data class ConfigResponse(
    val id: String,
    val data: JsonNode,
    val minVersion: ConfigVersionDto?,
    val maxVersion: ConfigVersionDto?,
) {
    companion object {
        fun from(config: ClientConfig) =
            ConfigResponse(
                id = config.id!!,
                data = jacksonObjectMapper().readTree(config.value),
                minVersion = ConfigVersionDto.of(config.minIosVersion, config.minAndroidVersion),
                maxVersion = ConfigVersionDto.of(config.maxIosVersion, config.maxAndroidVersion),
            )
    }
}

data class ConfigVersionDto(
    val ios: String,
    val android: String,
) {
    @get:JsonIgnore
    val iosAppVersion
        get() = AppVersion(ios)

    @get:JsonIgnore
    val androidAppVersion
        get() = AppVersion(android)

    companion object {
        fun of(iosAppVersion: AppVersion?, androidAppVersion: AppVersion?): ConfigVersionDto? {
            return if (iosAppVersion != null && androidAppVersion != null) {
                ConfigVersionDto(
                    ios = iosAppVersion.appVersion,
                    android = androidAppVersion.appVersion,
                )
            } else null
        }
    }
}
