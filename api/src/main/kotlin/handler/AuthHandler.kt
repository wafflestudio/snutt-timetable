package com.wafflestudio.snutt.handler

import com.wafflestudio.snutt.common.dto.OkResponse
import com.wafflestudio.snutt.middleware.SnuttRestApiNoAuthMiddleware
import com.wafflestudio.snutt.users.dto.EmailResponse
import com.wafflestudio.snutt.users.dto.FacebookLoginRequest
import com.wafflestudio.snutt.users.dto.GetMaskedEmailRequest
import com.wafflestudio.snutt.users.dto.LocalLoginRequest
import com.wafflestudio.snutt.users.dto.LocalRegisterRequest
import com.wafflestudio.snutt.users.dto.PasswordResetRequest
import com.wafflestudio.snutt.users.dto.SendEmailRequest
import com.wafflestudio.snutt.users.dto.SocialLoginRequest
import com.wafflestudio.snutt.users.dto.VerificationCodeRequest
import com.wafflestudio.snutt.users.service.UserService
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
            userService.loginFacebook(SocialLoginRequest(facebookLoginRequest.fbToken))
        }

    suspend fun loginFacebook(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.loginFacebook(socialLoginRequest)
        }

    suspend fun loginGoogle(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.loginGoogle(socialLoginRequest)
        }

    suspend fun loginKakao(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.loginKakao(socialLoginRequest)
        }

    suspend fun loginAppleLegacy(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.loginApple(socialLoginRequest)
        }

    suspend fun loginApple(req: ServerRequest): ServerResponse =
        handle(req) {
            val socialLoginRequest: SocialLoginRequest = req.awaitBodyOrNull() ?: throw ServerWebInputException("Invalid body")
            userService.loginApple(socialLoginRequest)
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
