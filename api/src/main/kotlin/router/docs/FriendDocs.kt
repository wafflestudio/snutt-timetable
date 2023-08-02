package com.wafflestudio.snu4t.router.docs

import com.wafflestudio.snu4t.common.dto.ListResponse
import com.wafflestudio.snu4t.friend.dto.FriendRequest
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
        path = "/v1/friends", method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "getFriends",
            parameters = [Parameter(`in` = ParameterIn.QUERY, name = "isAccepted", required = false)],
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = ListResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/friends", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "requestFriend",
            requestBody = RequestBody(
                content = [Content(schema = Schema(implementation = FriendRequest::class))],
                required = true,
            ),
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = ListResponse::class))])]
        ),
    ),
    RouterOperation(
        path = "/v1/friends/{friendId}/accept", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "acceptFriend",
            parameters = [Parameter(`in` = ParameterIn.PATH, name = "friendId", required = true)],
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema())])]
        ),
    ),
    RouterOperation(
        path = "/v1/friends/{friendId}/decline", method = [RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "declineFriend",
            parameters = [Parameter(`in` = ParameterIn.PATH, name = "friendId", required = true)],
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema())])]
        ),
    ),
    RouterOperation(
        path = "/v1/friends/{friendId}", method = [RequestMethod.DELETE], produces = [MediaType.APPLICATION_JSON_VALUE],
        operation = Operation(
            operationId = "breakFriend",
            parameters = [Parameter(`in` = ParameterIn.PATH, name = "friendId", required = true)],
            responses = [ApiResponse(responseCode = "200", content = [Content(schema = Schema())])]
        ),
    ),
)
annotation class FriendDocs()
