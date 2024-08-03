package com.wafflestudio.snu4t.handler

import com.wafflestudio.snu4t.bookmark.dto.BookmarkLectureModifyRequest
import com.wafflestudio.snu4t.bookmark.dto.BookmarkResponse
import com.wafflestudio.snu4t.bookmark.service.BookmarkService
import com.wafflestudio.snu4t.common.dto.ExistenceResponse
import com.wafflestudio.snu4t.common.enum.Semester
import com.wafflestudio.snu4t.middleware.SnuttRestApiDefaultMiddleware
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.awaitBody

@Component
class BookmarkHandler(
    private val bookmarkService: BookmarkService,
    snuttRestApiDefaultMiddleware: SnuttRestApiDefaultMiddleware,
) : ServiceHandler(handlerMiddleware = snuttRestApiDefaultMiddleware) {

    suspend fun getBookmarks(req: ServerRequest) = handle(req) {
        val userId: String = req.userId
        val year: Int = req.parseRequiredQueryParam("year")
        val semester: Semester = req.parseRequiredQueryParam("semester") { Semester.getOfValue(it.toInt()) }
        val bookmark = bookmarkService.getBookmark(userId, year, semester)
        val bookmarkLectureDtos = bookmarkService.convertBookmarkLecturesToBookmarkLectureDtos(bookmark.lectures)

        BookmarkResponse(
            year = bookmark.year,
            semester = bookmark.semester.value,
            lectures = bookmarkLectureDtos
        )
    }

    suspend fun existsBookmarkLecture(req: ServerRequest) = handle(req) {
        val userId: String = req.userId
        val lectureId = req.pathVariable("lectureId")
        ExistenceResponse(bookmarkService.existsBookmarkLecture(userId, lectureId))
    }

    suspend fun addLecture(req: ServerRequest) = handle(req) {
        val userId: String = req.userId
        val body = req.awaitBody<BookmarkLectureModifyRequest>()
        val lectureId = body.lectureId
        bookmarkService.addLecture(userId, lectureId)
        null
    }

    suspend fun deleteBookmark(req: ServerRequest) = handle(req) {
        val userId: String = req.userId
        val body = req.awaitBody<BookmarkLectureModifyRequest>()
        val lectureId = body.lectureId
        bookmarkService.deleteLecture(userId, lectureId)
        null
    }
}
