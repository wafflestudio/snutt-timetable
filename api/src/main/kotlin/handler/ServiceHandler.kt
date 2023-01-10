package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.middleware.Middleware
import com.wafflestudio.snu4t.middleware.plus
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait

abstract class ServiceHandler(val handlerMiddleware: Middleware = Middleware.NoOp) {
    protected suspend fun <T : Any> handle(
        req: ServerRequest,
        additionalMiddleware: Middleware = Middleware.NoOp,
        function: suspend () -> T?,
    ): ServerResponse {
        req.setContext((handlerMiddleware + additionalMiddleware).invoke(req, req.getContext()))
        return function()?.toResponse() ?: ServerResponse.ok().buildAndAwait()
    }

    private suspend fun <T : Any> T.toResponse(): ServerResponse {
        return ServerResponse.ok().bodyValue(this).awaitSingle()
    }
}
