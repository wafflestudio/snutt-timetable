package com.wafflestudio.snutt.bookmark.repository

import com.wafflestudio.snutt.bookmark.data.Bookmark
import com.wafflestudio.snutt.common.enum.Semester
import com.wafflestudio.snutt.common.extension.elemMatch
import com.wafflestudio.snutt.common.extension.isEqualTo
import com.wafflestudio.snutt.lectures.data.BookmarkLecture
import com.wafflestudio.snutt.lectures.data.Lecture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.bson.types.ObjectId
import org.springframework.data.mapping.toDotPath
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findModifyAndAwait
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.update

interface BookmarkCustomRepository {
    fun findAllContainsLectureId(
        year: Int,
        semester: Semester,
        lectureId: String,
    ): Flow<Bookmark>

    suspend fun findAndAddLectureByUserIdAndYearAndSemester(
        userId: String,
        year: Int,
        semester: Semester,
        lecture: BookmarkLecture,
    ): Bookmark

    suspend fun findAndDeleteLectureByUserIdAndYearAndSemesterAndLectureId(
        userId: String,
        year: Int,
        semester: Semester,
        lectureId: String,
    ): Bookmark

    suspend fun pullLecture(
        timeTableId: String,
        lectureId: String,
    )

    suspend fun updateLecture(
        bookmarkId: String,
        lecture: BookmarkLecture,
    ): Bookmark
}

class BookmarkCustomRepositoryImpl(private val reactiveMongoTemplate: ReactiveMongoTemplate) :
    BookmarkCustomRepository {
    override fun findAllContainsLectureId(
        year: Int,
        semester: Semester,
        lectureId: String,
    ): Flow<Bookmark> {
        return reactiveMongoTemplate.find<Bookmark>(
            Query.query(
                Bookmark::year isEqualTo year and
                    Bookmark::semester isEqualTo semester and
                    Bookmark::lectures elemMatch (BookmarkLecture::id isEqualTo lectureId),
            ),
        ).asFlow()
    }

    override suspend fun findAndAddLectureByUserIdAndYearAndSemester(
        userId: String,
        year: Int,
        semester: Semester,
        lecture: BookmarkLecture,
    ): Bookmark =
        reactiveMongoTemplate.update<Bookmark>().matching(
            Bookmark::userId isEqualTo userId and
                Bookmark::year isEqualTo year and
                Bookmark::semester isEqualTo semester,
        ).apply(
            Update().addToSet(Bookmark::lectures.toDotPath(), lecture),
        ).withOptions(
            FindAndModifyOptions.options().returnNew(true).upsert(true),
        ).findModifyAndAwait()

    override suspend fun findAndDeleteLectureByUserIdAndYearAndSemesterAndLectureId(
        userId: String,
        year: Int,
        semester: Semester,
        lectureId: String,
    ): Bookmark =
        reactiveMongoTemplate.update<Bookmark>().matching(
            Bookmark::userId isEqualTo userId and
                Bookmark::year isEqualTo year and
                Bookmark::semester isEqualTo semester,
        ).apply(
            Update().pull(Bookmark::lectures.toDotPath(), Query.query(Lecture::id isEqualTo lectureId)),
        ).findModifyAndAwait()

    override suspend fun pullLecture(
        timeTableId: String,
        lectureId: String,
    ) {
        reactiveMongoTemplate.update<Bookmark>().matching(
            Bookmark::id isEqualTo timeTableId,
        ).apply(
            Update().pull(Bookmark::lectures.toDotPath(), Query.query(BookmarkLecture::id isEqualTo lectureId)),
        ).findModifyAndAwait()
    }

    override suspend fun updateLecture(
        bookmarkId: String,
        lecture: BookmarkLecture,
    ): Bookmark {
        return reactiveMongoTemplate.update<Bookmark>().matching(
            Bookmark::id.isEqualTo(bookmarkId).and("lectures._id").isEqualTo(ObjectId(lecture.id)),
        ).apply(Update().set("""lectures.$""", lecture))
            .withOptions(FindAndModifyOptions.options().returnNew(true)).findModifyAndAwait()
    }
}
