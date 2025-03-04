package com.wafflestudio.snutt.handler

import com.wafflestudio.snutt.auth.AuthProvider
import com.wafflestudio.snutt.common.dto.OkResponse
import com.wafflestudio.snutt.common.extension.toZonedDateTime
import com.wafflestudio.snutt.middleware.SnuttRestApiDefaultMiddleware
import com.wafflestudio.snutt.users.data.User
import com.wafflestudio.snutt.users.dto.AuthProvidersCheckDto
import com.wafflestudio.snutt.users.dto.EmailVerificationResultDto
import com.wafflestudio.snutt.users.dto.LocalLoginRequest
import com.wafflestudio.snutt.users.dto.LogoutRequest
import com.wafflestudio.snutt.users.dto.PasswordChangeRequest
import com.wafflestudio.snutt.users.dto.SendEmailRequest
import com.wafflestudio.snutt.users.dto.SocialLoginRequest
import com.wafflestudio.snutt.users.dto.UserDto
import com.wafflestudio.snutt.users.dto.UserLegacyDto
import com.wafflestudio.snutt.users.dto.UserPatchRequest
import com.wafflestudio.snutt.users.dto.VerificationCodeRequest
import com.wafflestudio.snutt.users.service.UserNicknameService
import com.wafflestudio.snutt.users.service.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import org.springframework.web.server.ServerWebInputException

@Component
class UserHandler(
    private val userService: UserService,
    private val userNicknameService: UserNicknameService,
    snuttRestApiDefaultMiddleware: SnuttRestApiDefaultMiddleware,
) : ServiceHandler(snuttRestApiDefaultMiddleware) {
    suspend fun getUserMe(req: ServerRequest): ServerResponse =
        handle(req) {
            val userId = req.userId

            val user = userService.getUser(userId)
            buildUserDto(user)
        }

    suspend fun getUserInfo(req: ServerRequest): ServerResponse =
        handle(req) {
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

    suspend fun patchUserInfo(req: ServerRequest): ServerResponse =
        handle(req) {
            val userId = req.userId
            val body = req.awaitBody<UserPatchRequest>()

            val user = userService.patchUserInfo(userId, body)
            buildUserDto(user)
        }

    suspend fun deleteAccount(req: ServerRequest): ServerResponse =
        handle(req) {
            userService.update(req.getContext().user!!.copy(active = false))
            OkResponse()
        }

    suspend fun sendVerificationEmail(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            val email = req.awaitBody<SendEmailRequest>().email
            userService.sendVerificationCode(user, email)
            OkResponse()
        }

    suspend fun confirmEmailVerification(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            val code = req.awaitBody<VerificationCodeRequest>().code
            userService.verifyEmail(user, code)
            EmailVerificationResultDto(true)
        }

    suspend fun resetEmailVerification(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            userService.resetEmailVerification(user)
            EmailVerificationResultDto(false)
        }

    suspend fun getEmailVerification(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            EmailVerificationResultDto(user.isEmailVerified ?: false)
        }

    suspend fun attachLocal(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            val body = req.awaitBody<LocalLoginRequest>()
            userService.attachLocal(user, body)
        }

    suspend fun attachFacebook(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            val socialLoginRequest: SocialLoginRequest = req.awaitBody()
            userService.attachSocial(user, socialLoginRequest, AuthProvider.FACEBOOK)
        }

    suspend fun attachGoogle(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            val socialLoginRequest: SocialLoginRequest = req.awaitBody()
            userService.attachSocial(user, socialLoginRequest, AuthProvider.GOOGLE)
        }

    suspend fun attachKakao(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            val socialLoginRequest: SocialLoginRequest = req.awaitBody()
            userService.attachSocial(user, socialLoginRequest, AuthProvider.KAKAO)
        }

    suspend fun attachApple(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            val socialLoginRequest: SocialLoginRequest = req.awaitBody()
            userService.attachSocial(user, socialLoginRequest, AuthProvider.APPLE)
        }

    suspend fun detachFacebook(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            userService.detachSocial(user, AuthProvider.FACEBOOK)
        }

    suspend fun detachGoogle(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            userService.detachSocial(user, AuthProvider.GOOGLE)
        }

    suspend fun detachKakao(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            userService.detachSocial(user, AuthProvider.KAKAO)
        }

    suspend fun detachApple(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            userService.detachSocial(user, AuthProvider.APPLE)
        }

    suspend fun checkAuthProviders(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!

            AuthProvidersCheckDto(
                local = user.credential.localId != null,
                facebook = user.credential.fbName != null,
                google = user.credential.googleSub != null,
                kakao = user.credential.kakaoSub != null,
                apple = user.credential.appleSub != null,
            )
        }

    suspend fun changePassword(req: ServerRequest): ServerResponse =
        handle(req) {
            val user = req.getContext().user!!
            val body = req.awaitBody<PasswordChangeRequest>()
            userService.changePassword(user, body)
        }

    suspend fun logout(req: ServerRequest): ServerResponse =
        handle(req) {
            val userId = req.userId
            val logoutRequest: LogoutRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.logout(userId, logoutRequest)

            OkResponse()
        }

    private fun buildUserDto(user: User) =
        UserDto(
            id = user.id!!,
            isAdmin = user.isAdmin,
            regDate = user.regDate,
            notificationCheckedAt = user.notificationCheckedAt,
            email = user.email,
            localId = user.credential.localId,
            fbName = user.credential.fbName,
            nickname = userNicknameService.getNicknameDto(user.nickname),
        )
}
