package com.wafflestudio.snu4t.router

import com.wafflestudio.snu4t.handler.AuthHandler
import com.wafflestudio.snu4t.handler.TimeTableHandler
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Component
class MainRouter(
    private val timeTableHandler: TimeTableHandler,
    private val authHandler: AuthHandler,
) {
    @Bean
    fun route(): RouterFunction<ServerResponse> = coRouter {
        GET("/ping") { ServerResponse.ok().bodyValueAndAwait("pong") }
        "/v1/tables".nest {
            GET("", timeTableHandler::getBriefs)
        }
        "/v1/auth".nest {
            POST("/register_local", authHandler::registerLocal)
        }
    }
}
