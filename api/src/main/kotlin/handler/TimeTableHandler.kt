package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.middleware.Middleware
import com.wafflestudio.snu4t.timetables.service.TimeTableService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class TimeTableHandler(
    private val timeTableService: TimeTableService,
    snuttRestApiDefaultMiddleware: Middleware,
) : ServiceHandler(
    handlerMiddleware = snuttRestApiDefaultMiddleware
) {
    suspend fun getBriefs(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId

        timeTableService.getBriefs(userId = userId)
    }
}
