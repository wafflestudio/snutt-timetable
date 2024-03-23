package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.common.dto.ListResponse
import com.wafflestudio.snu4t.middleware.SnuttRestApiNoAuthMiddleware
import com.wafflestudio.snu4t.popup.dto.PopupResponse
import com.wafflestudio.snu4t.popup.service.PopupService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class PopupHandler(
    private val popupService: PopupService,
    snuttRestApiNoAuthMiddleware: SnuttRestApiNoAuthMiddleware,
) : ServiceHandler(snuttRestApiNoAuthMiddleware) {
    suspend fun getPopups(req: ServerRequest) = handle(req) {
        val clientInfo = req.clientInfo!!

        val popups = popupService.getPopups(clientInfo)

        ListResponse(
            content = popups.map(::PopupResponse),
            totalCount = popups.size,
            nextPageToken = null,
        )
    }
}
