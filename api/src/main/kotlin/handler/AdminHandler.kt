package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.clientconfig.dto.ConfigResponse
import com.wafflestudio.snu4t.clientconfig.dto.PatchConfigRequest
import com.wafflestudio.snu4t.clientconfig.dto.PostConfigRequest
import com.wafflestudio.snu4t.clientconfig.service.ClientConfigService
import com.wafflestudio.snu4t.common.dto.OkResponse
import com.wafflestudio.snu4t.middleware.SnuttRestAdminApiMiddleware
import com.wafflestudio.snu4t.notification.service.NotificationAdminService
import notification.dto.InsertNotificationRequest
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.awaitBody

@Component
class AdminHandler(
    private val notificationAdminService: NotificationAdminService,
    private val configService: ClientConfigService,
    snuttRestAdminApiMiddleware: SnuttRestAdminApiMiddleware,
) : ServiceHandler(snuttRestAdminApiMiddleware) {
    suspend fun insertNotification(req: ServerRequest) = handle(req) {
        val body = req.awaitBody<InsertNotificationRequest>()
        notificationAdminService.insertNotification(body)

        OkResponse()
    }

    suspend fun postConfig(req: ServerRequest) = handle(req) {
        val name = req.pathVariable("name")
        val body = req.awaitBody<PostConfigRequest>()

        val config = configService.postConfig(name, body)
        ConfigResponse.from(config)
    }

    suspend fun getConfigs(req: ServerRequest) = handle(req) {
        val name = req.pathVariable("name")

        val configs = configService.getConfigsByName(name)
        configs.map { ConfigResponse.from(it) }
    }

    suspend fun deleteConfig(req: ServerRequest) = handle(req) {
        val name = req.pathVariable("name")
        val configId = req.pathVariable("id")

        configService.deleteConfig(name, configId)
    }

    suspend fun patchConfig(req: ServerRequest) = handle(req) {
        val name = req.pathVariable("name")
        val configId = req.pathVariable("id")
        val body = req.awaitBody<PatchConfigRequest>()

        val config = configService.patchConfig(name, configId, body)
        ConfigResponse.from(config)
    }

    suspend fun postPopup(req: ServerRequest) = handle(req) {
        TODO()
    }
}
