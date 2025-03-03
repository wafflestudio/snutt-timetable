package com.wafflestudio.snutt.handler

import com.wafflestudio.snutt.middleware.SnuttRestApiDefaultMiddleware
import com.wafflestudio.snutt.notification.data.PushCategory
import com.wafflestudio.snutt.notification.dto.PushPreferenceResponse
import com.wafflestudio.snutt.notification.service.PushPreferenceService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class PushPreferenceHandler(
    private val pushPreferenceService: PushPreferenceService,
    snuttRestApiDefaultMiddleware: SnuttRestApiDefaultMiddleware,
) : ServiceHandler(
        handlerMiddleware = snuttRestApiDefaultMiddleware,
    ) {
    suspend fun getPushPreferences(req: ServerRequest) =
        handle(req) {
            val user = req.getContext().user!!
            val pushPreferences = pushPreferenceService.getPushPreferences(user)
            pushPreferences.map { PushPreferenceResponse(it) }
        }

    suspend fun enableLectureUpdate(req: ServerRequest) =
        handle(req) {
            val user = req.getContext().user!!
            pushPreferenceService.enablePush(user, PushCategory.LECTURE_UPDATE)
        }

    suspend fun disableLectureUpdate(req: ServerRequest) =
        handle(req) {
            val user = req.getContext().user!!
            pushPreferenceService.disablePush(user, PushCategory.LECTURE_UPDATE)
        }

    suspend fun enableVacancyNotification(req: ServerRequest) =
        handle(req) {
            val user = req.getContext().user!!
            pushPreferenceService.enablePush(user, PushCategory.VACANCY_NOTIFICATION)
        }

    suspend fun disableVacancyNotification(req: ServerRequest) =
        handle(req) {
            val user = req.getContext().user!!
            pushPreferenceService.disablePush(user, PushCategory.VACANCY_NOTIFICATION)
        }
}
