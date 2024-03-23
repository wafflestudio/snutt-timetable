package com.wafflestudio.snu4t.router.docs

import com.wafflestudio.snu4t.common.dto.ListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMethod

@RouterOperations(
    RouterOperation(
        path = "/v1/popups", method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "getPopups",
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = ListResponse::class))])]
        ),
    ),
)
annotation class PopupDocs
