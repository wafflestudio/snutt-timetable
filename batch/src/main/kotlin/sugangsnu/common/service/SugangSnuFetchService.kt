package com.wafflestudio.snu4t.sugangsnu.common.service

import com.wafflestudio.snu4t.common.enum.Semester
import com.wafflestudio.snu4t.lectures.data.Lecture
import com.wafflestudio.snu4t.sugangsnu.common.SugangSnuRepository
import com.wafflestudio.snu4t.sugangsnu.common.utils.SugangSnuClassTimeUtils
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface SugangSnuFetchService {
    suspend fun getSugangSnuLectures(year: Int, semester: Semester): List<Lecture>
}

@Service
class SugangSnuFetchServiceImpl(
    private val sugangSnuRepository: SugangSnuRepository,
) : SugangSnuFetchService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val quotaRegex = """(?<quota>\d+)(\s*\((?<quotaForCurrentStudent>\d+)\))?""".toRegex()

    override suspend fun getSugangSnuLectures(year: Int, semester: Semester): List<Lecture> {
        val koreanLectureXlsx = sugangSnuRepository.getSugangSnuLectures(year, semester, "ko")
        val englishLectureXlsx = sugangSnuRepository.getSugangSnuLectures(year, semester, "en")
        val koreanSheet = HSSFWorkbook(koreanLectureXlsx.asInputStream()).getSheetAt(0)
        val englishSheet = HSSFWorkbook(englishLectureXlsx.asInputStream()).getSheetAt(0)
        val fullSheet = koreanSheet.zip(englishSheet).map { (koreanRow, englishRow) -> koreanRow + englishRow }
        val columnNameIndex = fullSheet[2].associate { it.stringCellValue to it.columnIndex }
        return fullSheet.drop(2).map { row ->
            convertSugangSnuRowToLecture(row, columnNameIndex, year, semester)
        }.also {
            koreanLectureXlsx.release()
            englishLectureXlsx.release()
        }.map { lecture ->
            val extraLectureInfo =
                sugangSnuRepository.getLectureInfo(year, semester, lecture.courseNumber, lecture.lectureNumber)
            lecture.apply {
                classPlaceAndTimes = SugangSnuClassTimeUtils.convertTextToClassTimeObject(
                    extraLectureInfo.ltTime,
                    extraLectureInfo.ltRoom
                )
                category = extraLectureInfo.subInfo.sbjtFldNm ?: category
                if (extraLectureInfo.subInfo.departmentKorNm != null && extraLectureInfo.subInfo.majorKorNm != null) {
                    department = "${extraLectureInfo.subInfo.departmentKorNm}(${extraLectureInfo.subInfo.majorKorNm})"
                }
            }
        }
    }

    /*
    엑셀 항목 (2023/01/26): 교과구분, 개설대학, 개설학과, 이수과정, 학년, 교과목번호, 강좌번호, 교과목명,
    부제명, 학점, 강의, 실습, 수업교시, 수업형태, 강의실(동-호)(#연건, *평창), 주담당교수,
    장바구니신청, 신입생장바구니신청, 재학생장바구니신청, 정원, 수강신청인원, 비고, 강의언어, 개설상태,
     */
    private fun convertSugangSnuRowToLecture(
        row: List<Cell>,
        columnNameIndex: Map<String, Int>,
        year: Int,
        semester: Semester,
    ): Lecture {
        fun List<Cell>.getCellByColumnName(key: String): String =
            this[
                columnNameIndex.getOrElse(key) {
                    log.error("$key 와 매칭되는 excel 컬럼이 존재하지 않습니다.")
                    this.size
                }
            ].stringCellValue

        val classification = row.getCellByColumnName("교과구분")
        val college = row.getCellByColumnName("개설대학")
        val department = row.getCellByColumnName("개설학과")
        val academicCourse = row.getCellByColumnName("이수과정")
        val academicYear = row.getCellByColumnName("학년")
        val courseNumber = row.getCellByColumnName("교과목번호")
        val lectureNumber = row.getCellByColumnName("강좌번호")
        val courseTitle = row.getCellByColumnName("교과목명")
        val courseSubtitle = row.getCellByColumnName("부제명")
        val credit = row.getCellByColumnName("학점").toLong()
        val classTimeText = row.getCellByColumnName("수업교시")
        val location = row.getCellByColumnName("강의실(동-호)(#연건, *평창)")
        val instructor = row.getCellByColumnName("주담당교수")
        val (quota, quotaForCurrentStudent) = row.getCellByColumnName("정원")
            .takeIf { quotaRegex.matches(it) }
            ?.let { quotaRegex.find(it)!!.groups }
            ?.let { it["quota"]!!.value.toInt() to (it["quotaForCurrentStudent"]?.value?.toInt() ?: 0) } ?: (0 to 0)
        val remark = row.getCellByColumnName("비고")
        val registrationCount = row.getCellByColumnName("수강신청인원").toIntOrNull() ?: 0

        val classTimes =
            SugangSnuClassTimeUtils.convertTextToClassTimeObject(classTimeText.split("/"), location.split("/"))

        val courseFullTitle = if (courseSubtitle.isEmpty()) courseTitle else "$courseTitle ($courseSubtitle)"

        return Lecture(
            classification = classification,
            // null(과학교육계) 존재한다고 함 (old snutt에서 참고)
            department = department.replace("null", "").ifEmpty { college },
            academicYear = academicCourse.takeIf { academicCourse != "학사" } ?: academicYear,
            courseNumber = courseNumber,
            lectureNumber = lectureNumber,
            courseTitle = courseFullTitle,
            credit = credit,
            instructor = instructor,
            remark = remark,
            quota = quota,
            freshmanQuota = (quota - quotaForCurrentStudent).takeIf { it > 0 },
            year = year,
            semester = semester,
            category = "",
            classPlaceAndTimes = classTimes,
            registrationCount = registrationCount
        )
    }
}
