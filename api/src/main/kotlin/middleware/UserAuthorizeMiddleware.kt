package com.wafflestudio.snu4t.middleware

import com.wafflestudio.snu4t.common.exception.NoUserTokenException
import com.wafflestudio.snu4t.handler.RequestContext
import com.wafflestudio.snu4t.users.service.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class UserAuthorizeMiddleware(private val userService: UserService) : Middleware {
    override suspend fun invoke(req: ServerRequest, context: RequestContext): RequestContext {
        val token = req.headers().firstHeader("x-access-token") ?: throw NoUserTokenException
        val currentUser = userService.getUserByCredentialHash(token)
        return context.copy(user = currentUser)
    }
}
