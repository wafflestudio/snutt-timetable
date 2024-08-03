package com.wafflestudio.snu4t.router.docs

import com.wafflestudio.snu4t.common.dto.OkResponse
import com.wafflestudio.snu4t.users.dto.EmailResponse
import com.wafflestudio.snu4t.users.dto.FacebookLoginRequest
import com.wafflestudio.snu4t.users.dto.GetMaskedEmailRequest
import com.wafflestudio.snu4t.users.dto.LocalLoginRequest
import com.wafflestudio.snu4t.users.dto.LocalRegisterRequest
import com.wafflestudio.snu4t.users.dto.LoginResponse
import com.wafflestudio.snu4t.users.dto.LogoutRequest
import com.wafflestudio.snu4t.users.dto.PasswordResetRequest
import com.wafflestudio.snu4t.users.dto.SendEmailRequest
import com.wafflestudio.snu4t.users.dto.SocialLoginRequest
import com.wafflestudio.snu4t.users.dto.VerificationCodeRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMethod

@RouterOperations(
    RouterOperation(
        path = "/v1/auth/register_local", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "registerLocal",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = LocalRegisterRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = LoginResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/login_local", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "loginLocal",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = LocalLoginRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = LoginResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/login_fb", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "loginFacebookLegacy",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = FacebookLoginRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = LoginResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/login/facebook", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "loginFacebook",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = SocialLoginRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = LoginResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/login/google", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "loginGoogle",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = SocialLoginRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = LoginResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/login/kakao", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "loginKakao",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = SocialLoginRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = LoginResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/logout", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "logout",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = LogoutRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/find_id", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "findId",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = SendEmailRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/password/reset", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "resetPassword",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = PasswordResetRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/password/reset/email/check", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "getMaskedEmail",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = GetMaskedEmailRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = EmailResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/password/reset/email/send", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "sendResetPasswordCode",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = SendEmailRequest::class))]),
            responses = [
                ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])
            ]
        ),
    ),
    RouterOperation(
        path = "/v1/auth/password/reset/verification/code", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "verifyResetPasswordCode",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = VerificationCodeRequest::class))]),
            responses = [
                ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])
            ]
        ),
    )
)
annotation class AuthDocs
