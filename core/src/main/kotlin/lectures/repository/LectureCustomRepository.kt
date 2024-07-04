package com.wafflestudio.snu4t.lectures.repository

import com.wafflestudio.snu4t.common.extension.isEqualTo
import com.wafflestudio.snu4t.lectures.data.ClassPlaceAndTime
import com.wafflestudio.snu4t.lectures.data.Lecture
import com.wafflestudio.snu4t.lectures.dto.SearchDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.elemMatch
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.query.ne
import org.springframework.data.mongodb.core.query.nin
import org.springframework.data.mongodb.core.query.not
import org.springframework.data.mongodb.core.query.regex

interface LectureCustomRepository {
    fun searchLectures(searchCondition: SearchDto): Flow<Lecture>
}

class LectureCustomRepositoryImpl(
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
) : LectureCustomRepository {
    companion object {
        private val placeRegex = """^\d+-\d+(?:-\d+)?(?:-\d+)?$""".toRegex()
        private val buildingRegex = """^\d+(?:-\d+)?동$""".toRegex()
    }

    override fun searchLectures(searchCondition: SearchDto): Flow<Lecture> = reactiveMongoTemplate.find<Lecture>(
        Query.query(
            Criteria().andOperator(
                listOfNotNull(
                    Lecture::year isEqualTo searchCondition.year and Lecture::semester isEqualTo searchCondition.semester,
                    searchCondition.query?.let { makeSearchCriteriaFromQuery(it) },
                    searchCondition.credit?.takeIf { it.isNotEmpty() }?.let { Lecture::credit inValues it },
                    searchCondition.academicYear?.takeIf { it.isNotEmpty() }?.let { Lecture::academicYear inValues it },
                    searchCondition.courseNumber?.takeIf { it.isNotEmpty() }?.let { Lecture::courseNumber inValues it },
                    searchCondition.classification?.takeIf { it.isNotEmpty() }?.let { Lecture::classification inValues it },
                    searchCondition.category?.takeIf { it.isNotEmpty() }?.let { Lecture::category inValues it },
                    searchCondition.department?.takeIf { it.isNotEmpty() }?.let { Lecture::department inValues it },
                    searchCondition.times?.takeIf { it.isNotEmpty() }?.let { searchTimes ->
                        Criteria().andOperator(
                            Lecture::classPlaceAndTimes ne listOf(),
                            // 수업시간 하나라도 제시한 시간대들에 포함이 되지 않는 경우가 존재하면 안됨
                            Lecture::classPlaceAndTimes.not().elemMatch(
                                Criteria().andOperator(
                                    searchTimes.map { searchTime ->
                                        Criteria().orOperator(
                                            ClassPlaceAndTime::day.ne(searchTime.day),
                                            ClassPlaceAndTime::startMinute.lt(searchTime.startMinute),
                                            ClassPlaceAndTime::endMinute.gt(searchTime.endMinute)
                                        )
                                    }
                                )
                            )
                        )
                    },
                    searchCondition.timesToExclude?.takeIf { it.isNotEmpty() }?.let { excludeTimes ->
                        // 수업시간들과 제시한 시간대들 중 하나라도 겹치는 경우가 존재하면 안됨
                        Criteria().andOperator(
                            Lecture::classPlaceAndTimes ne listOf(),
                            Lecture::classPlaceAndTimes.not().elemMatch(
                                Criteria().orOperator(
                                    excludeTimes.map { time ->
                                        Criteria().andOperator(
                                            ClassPlaceAndTime::day.isEqualTo(time.day),
                                            ClassPlaceAndTime::startMinute.lt(time.endMinute),
                                            ClassPlaceAndTime::endMinute.gt(time.startMinute)
                                        )
                                    }
                                )
                            )
                        )
                    },
                    *searchCondition.etcTags.orEmpty().map { etcTag ->
                        when (etcTag) {
                            "E" -> Lecture::remark regex ".*ⓔ.*"
                            "MO" -> Lecture::remark regex ".*ⓜⓞ.*"
                            else -> null
                        }
                    }.toTypedArray()
                ),
            )
        )
            .with(searchCondition.sortBy)
            .skip(searchCondition.offset).limit(searchCondition.limit)
    ).asFlow()

    private fun makeSearchCriteriaFromQuery(query: String): Criteria =
        Criteria().andOperator(
            query.split(' ').map { keyword ->
                val fuzzyKeyword = keyword.toCharArray().joinToString(".*") { Regex.escape(it.toString()) }
                when {
                    keyword == "전공" -> Lecture::classification.inValues("전선", "전필")
                    keyword in listOf("석박", "대학원") -> Lecture::academicYear.inValues("석사", "박사", "석박사통합")
                    keyword in listOf("학부", "학사") -> Lecture::academicYear.nin("석사", "박사", "석박사통합")
                    keyword == "체육" -> Lecture::category isEqualTo "체육"
                    keyword in listOf("영강", "영어강의") -> Lecture::remark regex ".*ⓔ.*"
                    keyword in listOf("군휴학", "군휴학원격") -> Lecture::remark regex ".*ⓜⓞ.*"

                    placeRegex.matches(keyword) || buildingRegex.matches(keyword) -> {
                        val placeKeyword = keyword.replace("동", "")
                        Lecture::classPlaceAndTimes elemMatch Criteria().orOperator(
                            ClassPlaceAndTime::place.regex("^${Regex.escape(placeKeyword)}[^\\d]*", "i")
                        )
                    }

                    keyword.hasKorean() -> Criteria().orOperator(
                        listOfNotNull(
                            Lecture::courseTitle.regex(fuzzyKeyword, "i"),
                            Lecture::category.regex(fuzzyKeyword, "i"),
                            Lecture::instructor isEqualTo keyword,
                            Lecture::academicYear isEqualTo keyword,
                            Lecture::classification isEqualTo keyword,
                            when (keyword.last()) {
                                /*
                                '컴공과', '전기과' 등으로 검색할 때, 실제 학과명은 '컴퓨터공학부', '전기공학부'이므로 검색이 안됨.
                                만약 '과' 혹은 '부'로 끝나는 단어라면 regex의 마지막 단어를 빼버린다.
                                */
                                '과', '부' -> {
                                    val fuzzyWithoutLastChar = keyword.dropLast(1).toCharArray()
                                        .joinToString(".*") { Regex.escape(it.toString()) }
                                    Lecture::department.regex("^$fuzzyWithoutLastChar", "i")
                                }
                                // 마지막 글자가 '학'이라면 해당 학과의 수업이 모두 포함될 확률이 높다. 수학, 물리학, 경제학 etc
                                '학' -> null
                                else -> Lecture::department.regex("^$fuzzyKeyword", "i")
                            }
                        )
                    )

                    else -> Criteria().orOperator(
                        Lecture::courseTitle.regex(Regex.escape(keyword), "i"),
                        Lecture::instructor.regex(Regex.escape(keyword), "i"),
                        Lecture::courseNumber isEqualTo keyword,
                        Lecture::lectureNumber isEqualTo keyword,
                    )
                }
            }
        )

    private fun Char.isKoreanLetter(): Boolean {
        return this in '가'..'힣'
    }

    private fun String.hasKorean(): Boolean {
        return this.isNotEmpty() && this.map { it.isKoreanLetter() }.reduce { acc, c -> acc || c }
    }
}
