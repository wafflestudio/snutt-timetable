package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.auth.AuthProvider
import com.wafflestudio.snu4t.common.dto.OkResponse
import com.wafflestudio.snu4t.middleware.SnuttRestApiNoAuthMiddleware
import com.wafflestudio.snu4t.users.dto.EmailResponse
import com.wafflestudio.snu4t.users.dto.FacebookLoginRequest
import com.wafflestudio.snu4t.users.dto.GetMaskedEmailRequest
import com.wafflestudio.snu4t.users.dto.LocalLoginRequest
import com.wafflestudio.snu4t.users.dto.LocalRegisterRequest
import com.wafflestudio.snu4t.users.dto.LogoutRequest
import com.wafflestudio.snu4t.users.dto.PasswordResetRequest
import com.wafflestudio.snu4t.users.dto.SendEmailRequest
import com.wafflestudio.snu4t.users.dto.SocialLoginRequest
import com.wafflestudio.snu4t.users.dto.VerificationCodeRequest
import com.wafflestudio.snu4t.users.service.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import org.springframework.web.server.ServerWebInputException

@Component
class AuthHandler(
    private val userService: UserService,
    snuttRestApiNoAuthMiddleware: SnuttRestApiNoAuthMiddleware,
) : ServiceHandler(snuttRestApiNoAuthMiddleware) {
    suspend fun registerLocal(req: ServerRequest): ServerResponse =
        handle(req) {
            val localRegisterRequest: LocalRegisterRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.registerLocal(localRegisterRequest)
        }

    suspend fun loginLocal(req: ServerRequest): ServerResponse =
        handle(req) {
            val localLoginRequest: LocalLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.loginLocal(localLoginRequest)
        }

    suspend fun loginFacebookLegacy(req: ServerRequest): ServerResponse =
        handle(req) {
            val facebookLoginRequest: FacebookLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.socialLogin(SocialLoginRequest(facebookLoginRequest.fbToken), AuthProvider.FACEBOOK)
        }

    suspend fun loginFacebook(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.socialLogin(socialLoginRequest, AuthProvider.FACEBOOK)
        }

    suspend fun loginGoogle(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.socialLogin(socialLoginRequest, AuthProvider.GOOGLE)
        }

    suspend fun loginKakao(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.socialLogin(socialLoginRequest, AuthProvider.KAKAO)
        }

    suspend fun loginApple(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.socialLogin(socialLoginRequest, AuthProvider.APPLE)
        }

    suspend fun logout(req: ServerRequest): ServerResponse =
        handle(req) {
            val userId = req.userId
            val logoutRequest: LogoutRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.logout(userId, logoutRequest)

            OkResponse()
        }

    suspend fun findId(req: ServerRequest): ServerResponse =
        handle(req) {
            val email = req.awaitBody<SendEmailRequest>().email
            userService.sendLocalIdToEmail(email)
            OkResponse()
        }

    suspend fun sendResetPasswordCode(req: ServerRequest): ServerResponse =
        handle(req) {
            val email = req.awaitBody<SendEmailRequest>().email
            userService.sendResetPasswordCode(email)
            OkResponse()
        }

    suspend fun verifyResetPasswordCode(req: ServerRequest): ServerResponse =
        handle(req) {
            val body = req.awaitBody<VerificationCodeRequest>()
            userService.verifyResetPasswordCode(body.userId!!, body.code)
            OkResponse()
        }

    suspend fun getMaskedEmail(req: ServerRequest): ServerResponse =
        handle(req) {
            val id = req.awaitBody<GetMaskedEmailRequest>().userId
            EmailResponse(userService.getMaskedEmail(id))
        }

    suspend fun resetPassword(req: ServerRequest): ServerResponse =
        handle(req) {
            val body = req.awaitBody<PasswordResetRequest>()
            userService.resetPassword(body.userId, body.password, body.code)
            OkResponse()
        }
}
