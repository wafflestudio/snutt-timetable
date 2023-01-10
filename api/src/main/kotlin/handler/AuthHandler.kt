package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.middleware.Middleware
import com.wafflestudio.snu4t.users.dto.LocalRegisterRequest
import com.wafflestudio.snu4t.users.service.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import org.springframework.web.server.ServerWebInputException

@Component
class AuthHandler(
    private val userService: UserService,
    snuttRestApiNoAuthMiddleware: Middleware,
) : ServiceHandler(snuttRestApiNoAuthMiddleware) {
    suspend fun registerLocal(req: ServerRequest): ServerResponse = handle(req) {
        val localRegisterRequest: LocalRegisterRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
        userService.registerLocal(localRegisterRequest)
    }
}
