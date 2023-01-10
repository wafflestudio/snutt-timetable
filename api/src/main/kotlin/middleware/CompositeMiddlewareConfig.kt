package com.wafflestudio.snu4t.middleware

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CompositeMiddlewareConfig {
    @Bean("snuttRestApiDefaultMiddleware")
    fun snuttRestApiDefaultMiddleware(
        apiKeyMiddleware: ApiKeyMiddleware,
        userAuthorizeMiddleware: UserAuthorizeMiddleware,
        nativeClientInfoMiddleware: NativeClientInfoMiddleware,
    ): Middleware = apiKeyMiddleware + userAuthorizeMiddleware + nativeClientInfoMiddleware

    @Bean("snuttRestApiNoAuthMiddleware")
    fun snuttRestApiNoAuthMiddleware(
        apiKeyMiddleware: ApiKeyMiddleware,
        nativeClientInfoMiddleware: NativeClientInfoMiddleware,
    ): Middleware = apiKeyMiddleware + nativeClientInfoMiddleware
}
