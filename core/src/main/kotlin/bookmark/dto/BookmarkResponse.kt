package com.wafflestudio.snu4t.bookmark.dto

import com.wafflestudio.snu4t.lectures.dto.BookmarkLectureDto

class BookmarkResponse(
    val year: Int,
    val semester: Int,
    val lectures: List<BookmarkLectureDto>,
)
