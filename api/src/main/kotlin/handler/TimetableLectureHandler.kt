package com.wafflestudio.snu4t.handler

import com.fasterxml.jackson.annotation.JsonProperty
import com.wafflestudio.snu4t.middleware.SnuttRestApiDefaultMiddleware
import com.wafflestudio.snu4t.timetables.dto.TimetableLegacyDto
import com.wafflestudio.snu4t.timetables.dto.request.CustomTimetableLectureAddLegacyRequestDto
import com.wafflestudio.snu4t.timetables.dto.request.TimetableLectureModifyLegacyRequestDto
import com.wafflestudio.snu4t.timetables.service.TimetableLectureService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.awaitBodyOrNull

@Component
class TimetableLectureHandler(
    private val timetableLectureService: TimetableLectureService,
    snuttRestApiDefaultMiddleware: SnuttRestApiDefaultMiddleware,
) : ServiceHandler(
    handlerMiddleware = snuttRestApiDefaultMiddleware
) {
    suspend fun addCustomLecture(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId
        val timetableId = req.pathVariable("timetableId")
        val customTimetable = req.awaitBody<CustomTimetableLectureAddLegacyRequestDto>()
        val isForced = customTimetable.isForced

        timetableLectureService.addCustomTimetableLecture(
            userId = userId,
            timetableId = timetableId,
            timetableLectureRequest = customTimetable,
            isForced = isForced,
        ).let(::TimetableLegacyDto)
    }

    suspend fun addLecture(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId
        val timetableId = req.pathVariable("timetableId")
        val lectureId = req.pathVariable("lectureId")
        val isForced = req.awaitBodyOrNull<ForcedReq>()?.isForced ?: false

        timetableLectureService.addLecture(
            userId = userId,
            timetableId = timetableId,
            lectureId = lectureId,
            isForced = isForced,
        ).let(::TimetableLegacyDto)
    }

    suspend fun resetTimetableLecture(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId
        val timetableId = req.pathVariable("timetableId")
        val timetableLectureId = req.pathVariable("timetableLectureId")
        val isForced = req.awaitBodyOrNull<ForcedReq>()?.isForced ?: false

        timetableLectureService.resetTimetableLecture(
            userId = userId,
            timetableId = timetableId,
            timetableLectureId = timetableLectureId,
            isForced,
        ).let(::TimetableLegacyDto)
    }

    suspend fun modifyTimetableLecture(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId
        val timetableId = req.pathVariable("timetableId")
        val timetableLectureId = req.pathVariable("timetableLectureId")
        val modifyRequestDto = req.awaitBody<TimetableLectureModifyLegacyRequestDto>()
        val isForced = modifyRequestDto.isForced

        timetableLectureService.modifyTimetableLecture(
            userId = userId,
            timetableId = timetableId,
            timetableLectureId = timetableLectureId,
            modifyTimetableLectureRequestDto = modifyRequestDto,
            isForced = isForced,
        ).let(::TimetableLegacyDto)
    }

    suspend fun deleteTimetableLecture(req: ServerRequest): ServerResponse = handle(req) {
        val userId = req.userId
        val timetableId = req.pathVariable("timetableId")
        val timetableLectureId = req.pathVariable("timetableLectureId")

        timetableLectureService.deleteTimetableLecture(
            userId = userId,
            timetableId = timetableId,
            timetableLectureId = timetableLectureId,
        ).let(::TimetableLegacyDto)
    }

    data class ForcedReq(
        @JsonProperty("is_forced")
        val isForced: Boolean?
    )
}
