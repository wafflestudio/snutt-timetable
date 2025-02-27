package com.wafflestudio.snutt.middleware

import com.wafflestudio.snutt.common.exception.NoUserTokenException
import com.wafflestudio.snutt.common.exception.UserNotAdminException
import com.wafflestudio.snutt.handler.RequestContext
import com.wafflestudio.snutt.users.service.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class AdminApiMiddleware(private val userService: UserService) : Middleware {
    override suspend fun invoke(
        req: ServerRequest,
        context: RequestContext,
    ): RequestContext {
        val token = req.headers().firstHeader("x-access-token") ?: throw NoUserTokenException
        val currentUser = userService.getUserByCredentialHash(token)

        if (!currentUser.isAdmin) {
            throw UserNotAdminException
        }

        return context.copy(user = currentUser)
    }
}
