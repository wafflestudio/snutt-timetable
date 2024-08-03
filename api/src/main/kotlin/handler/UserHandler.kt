package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.common.dto.OkResponse
import com.wafflestudio.snu4t.common.extension.toZonedDateTime
import com.wafflestudio.snu4t.middleware.SnuttRestApiDefaultMiddleware
import com.wafflestudio.snu4t.users.data.User
import com.wafflestudio.snu4t.users.dto.EmailVerificationResultDto
import com.wafflestudio.snu4t.users.dto.LocalLoginRequest
import com.wafflestudio.snu4t.users.dto.PasswordChangeRequest
import com.wafflestudio.snu4t.users.dto.SendEmailRequest
import com.wafflestudio.snu4t.users.dto.UserDto
import com.wafflestudio.snu4t.users.dto.UserLegacyDto
import com.wafflestudio.snu4t.users.dto.UserPatchRequest
import com.wafflestudio.snu4t.users.dto.VerificationCodeRequest
import com.wafflestudio.snu4t.users.service.UserNicknameService
import com.wafflestudio.snu4t.users.service.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody

@Component
class UserHandler(
    private val userService: UserService,
    private val userNicknameService: UserNicknameService,
    snuttRestApiDefaultMiddleware: SnuttRestApiDefaultMiddleware,
) : ServiceHandler(snuttRestApiDefaultMiddleware) {
    suspend fun getUserMe(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId

        val user = userService.getUser(userId)
        buildUserDto(user)
    }

    suspend fun getUserInfo(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId
        val user = userService.getUser(userId)

        UserLegacyDto(
            isAdmin = user.isAdmin,
            regDate = user.regDate.toZonedDateTime(),
            notificationCheckedAt = user.notificationCheckedAt.toZonedDateTime(),
            email = user.email,
            localId = user.credential.localId,
            fbName = user.credential.fbName,
        )
    }

    suspend fun patchUserInfo(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId
        val body = req.awaitBody<UserPatchRequest>()

        val user = userService.patchUserInfo(userId, body)
        buildUserDto(user)
    }

    suspend fun deleteAccount(req: ServerRequest): ServerResponse = handle(req) {
        userService.update(req.getContext().user!!.copy(active = false))
        OkResponse()
    }

    suspend fun sendVerificationEmail(req: ServerRequest): ServerResponse = handle(req) {
        val user = req.getContext().user!!
        val email = req.awaitBody<SendEmailRequest>().email
        userService.sendVerificationCode(user, email)
        OkResponse()
    }

    suspend fun confirmEmailVerification(req: ServerRequest): ServerResponse = handle(req) {
        val user = req.getContext().user!!
        val code = req.awaitBody<VerificationCodeRequest>().code
        userService.verifyEmail(user, code)
        EmailVerificationResultDto(true)
    }

    suspend fun resetEmailVerification(req: ServerRequest): ServerResponse = handle(req) {
        val user = req.getContext().user!!
        userService.resetEmailVerification(user)
        EmailVerificationResultDto(false)
    }

    suspend fun getEmailVerification(req: ServerRequest): ServerResponse = handle(req) {
        val user = req.getContext().user!!
        EmailVerificationResultDto(user.isEmailVerified ?: false)
    }

    suspend fun attachLocal(req: ServerRequest): ServerResponse = handle(req) {
        val user = req.getContext().user!!
        val body = req.awaitBody<LocalLoginRequest>()
        userService.attachLocal(user, body)
    }

    suspend fun changePassword(req: ServerRequest): ServerResponse = handle(req) {
        val user = req.getContext().user!!
        val body = req.awaitBody<PasswordChangeRequest>()
        userService.changePassword(user, body)
    }

    private fun buildUserDto(user: User) = UserDto(
        id = user.id!!,
        isAdmin = user.isAdmin,
        regDate = user.regDate,
        notificationCheckedAt = user.notificationCheckedAt,
        email = user.email,
        localId = user.credential.localId,
        fbName = user.credential.fbName,
        nickname = userNicknameService.getNicknameDto(user.nickname!!),
    )
}
