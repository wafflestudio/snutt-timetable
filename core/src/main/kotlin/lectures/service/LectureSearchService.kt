package com.wafflestudio.snu4t.lectures.service

import com.wafflestudio.snu4t.common.addInWhereIfNotEmpty
import com.wafflestudio.snu4t.lectures.dto.SearchLectureQuery
import com.wafflestudio.snu4t.lectures.dto.SearchLectureRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

interface LectureSearchService {
    suspend fun searchLecture(request: SearchLectureRequest)
}

@Service
class LectureSearchServiceImpl : LectureSearchService {
    override suspend fun searchLecture(request: SearchLectureRequest) {

    }

    private fun SearchLectureQuery.makeMongoQueryFromLectureQuery(): Query {
        val query = Query()

        query.addCriteria(Criteria.where("year").`is`(year))
        query.addCriteria(Criteria.where("semester").`is`(semester))

        title?.let {
            query.addCriteria(Criteria.where("course_title").regex(".*$it.*", "i"))
        }
        query.addInWhereIfNotEmpty("credit", credit)
        query.addInWhereIfNotEmpty("instructor", instructor)
        query.addInWhereIfNotEmpty("academic_year", academicYear)
        query.addInWhereIfNotEmpty("course_number", courseNumber)
        query.addInWhereIfNotEmpty("classification", classification)
        query.addInWhereIfNotEmpty("category", category)
        query.addInWhereIfNotEmpty("department", department)

        if (!timeMask.isNullOrEmpty()) {
            if (timeMask.size != 7) {
                throw InvalidLectureTimemaskError()
            }

            val lectureTimeMaskNotZeroList = mutableListOf<Criteria>()
            val lectureTimeMaskMatchList = mutableListOf<Criteria>()

            for (i in 0 until 7) {
                lectureTimeMaskNotZeroList.add(Criteria.where("class_time_mask.$i").ne(0))
                lectureTimeMaskMatchList.add(Criteria.where("class_time_mask.$i").bits().allClear((~(it[i]) shl 1) ushr 1))
            }

            query.addCriteria(
                Criteria().andOperator(
                Criteria().orOperator(*lectureTimeMaskNotZeroList.toTypedArray()),
                Criteria().andOperator(*lectureTimeMaskMatchList.toTypedArray())
            ))
        }

        title?.let {
            query.addCriteria(makeSearchQueryFromTitle(it))
        }

        etc?.let {
            query.addCriteria(RefLectureQueryEtcTagService.getMQueryFromEtcTagList(it))
        }

        return query
    }

    private fun makeSearchQueryFromTitle(title: String): Criteria {
        val words = title.split(' ')

        val andQueryList = words.map { word ->
            val orQueryList = when (word) {
                "전공" -> listOf(Criteria.where("classification").`in`("전선", "전필"))
                "석박", "대학원" -> listOf(Criteria.where("academic_year").`in`("석사", "박사", "석박사통합"))
                "학부", "학사" -> listOf(Criteria.where("academic_year").nin("석사", "박사", "석박사통합"))
                "체육" -> listOf(Criteria.where("category").`is`("체육"))
                "영강", "영어강의" -> listOf(RefLectureQueryEtcTagService.getMQueryFromEtcTag(EtcTagEnum.ENGLISH_LECTURE))
                "군휴학", "군휴학원격" -> listOf(RefLectureQueryEtcTagService.getMQueryFromEtcTag(EtcTagEnum.MILITARY_REMOTE_LECTURE))
                else -> {
                    getCreditFromString(word)?.let { credit ->
                        listOf(Criteria.where("credit").`is`(credit))
                    } ?: run {
                        if (isHangulInString(word)) {
                            val regex = makeLikeRegEx(word)
                            listOf(
                                Criteria.where("course_title").regex(regex, "i"),
                                Criteria.where("instructor").`is`(word),
                                Criteria.where("category").regex(regex, "i"),
                                Criteria.where("department").apply {
                                    val lastChar = word.last()
                                    if (lastChar != '학') {
                                        if (lastChar == '과' || lastChar == '부') {
                                            regex("^${regex.dropLast(1)}", "i")
                                        } else {
                                            regex("^$regex", "i")
                                        }
                                    }
                                },
                                Criteria.where("classification").`is`(word),
                                Criteria.where("academic_year").`is`(word)
                            )
                        } else {
                            val regex = word
                            listOf(
                                Criteria.where("course_title").regex(regex, "i"),
                                Criteria.where("instructor").regex(regex, "i"),
                                Criteria.where("course_number").`is`(word),
                                Criteria.where("lecture_number").`is`(word)
                            )
                        }
                    }
                }
            }
            Criteria().orOperator(*orQueryList.toTypedArray())
        }

        return Criteria().andOperator(*andQueryList.toTypedArray())
    }
}
