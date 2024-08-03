package com.wafflestudio.snu4t.router.docs

import com.wafflestudio.snu4t.common.dto.OkResponse
import com.wafflestudio.snu4t.users.dto.EmailVerificationResultDto
import com.wafflestudio.snu4t.users.dto.LocalLoginRequest
import com.wafflestudio.snu4t.users.dto.PasswordChangeRequest
import com.wafflestudio.snu4t.users.dto.SendEmailRequest
import com.wafflestudio.snu4t.users.dto.TokenResponse
import com.wafflestudio.snu4t.users.dto.UserDto
import com.wafflestudio.snu4t.users.dto.UserLegacyDto
import com.wafflestudio.snu4t.users.dto.UserPatchRequest
import com.wafflestudio.snu4t.users.dto.VerificationCodeRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
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
        path = "/v1/user/device/{id}", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "registerLocal",
            parameters = [
                Parameter(`in` = ParameterIn.PATH, name = "fcmRegistrationId", required = true)
            ],
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/device/{id}", method = [RequestMethod.DELETE], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "registerLocal",
            parameters = [
                Parameter(`in` = ParameterIn.PATH, name = "fcmRegistrationId", required = true)
            ],
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/users/me", method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "getUserMe",
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = UserDto::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/users/me", method = [RequestMethod.PATCH], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "patchUserInfo",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = UserPatchRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = UserDto::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/info", method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "getUserInfo",
            description = "GET /v1/users/me 사용을 권장",
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = UserLegacyDto::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/account", method = [RequestMethod.DELETE], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "deleteAccount",
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/email/verification", method = [RequestMethod.DELETE], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "resetEmailVerification",
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = EmailVerificationResultDto::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/email/verification", method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "getEmailVerification",
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = EmailVerificationResultDto::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/email/verification", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "sendVerificationEmail",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = SendEmailRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = OkResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/email/verification/code", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "confirmEmailVerification",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = VerificationCodeRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = EmailVerificationResultDto::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/password", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "attachLocal",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = LocalLoginRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = TokenResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/user/password", method = [RequestMethod.PUT], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "changePassword",
            requestBody = RequestBody(content = [Content(schema = Schema(implementation = PasswordChangeRequest::class))]),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = TokenResponse::class))])]
        ),
    ),
)
annotation class UserDocs
