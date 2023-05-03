package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.lectures.dto.SearchLectureRequest
import com.wafflestudio.snu4t.lectures.service.LectureSearchService
import com.wafflestudio.snu4t.lectures.service.LectureService
import com.wafflestudio.snu4t.middleware.SnuttRestApiDefaultMiddleware
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.awaitBody

@Component
class SearchLectureHandler(
    private val lectureSearchService: LectureSearchService,
    snuttRestApiDefaultMiddleware: SnuttRestApiDefaultMiddleware
) : ServiceHandler(handlerMiddleware = snuttRestApiDefaultMiddleware) {
    suspend fun searchLecture(req: ServerRequest) = handle(req) {
        val body = req.awaitBody<SearchLectureRequest>()

        lectureSearchService.searchLecture(body)
    }
}
