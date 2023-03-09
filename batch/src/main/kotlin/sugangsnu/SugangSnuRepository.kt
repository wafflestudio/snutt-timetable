package com.wafflestudio.snu4t.sugangsnu

import com.wafflestudio.snu4t.common.enum.Semester
import com.wafflestudio.snu4t.sugangsnu.api.SugangSnuApi
import com.wafflestudio.snu4t.sugangsnu.data.SugangSnuCoursebookCondition
import com.wafflestudio.snu4t.sugangsnu.enum.LectureCategory
import com.wafflestudio.snu4t.sugangsnu.utils.toSugangSnuSearchString
import org.springframework.core.io.buffer.PooledDataBuffer
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@Component
class SugangSnuRepository(
    private val sugangSnuApi: SugangSnuApi,
) {
    companion object {
        const val SUGANG_SNU_COURSEBOOK_PATH = "/sugang/cc/cc100ajax.action"
        const val DEFAULT_COURSEBOOK_PARAMS = "openUpDeptCd=&openDeptCd="
        const val SUGANG_SNU_LECTURE_PATH = "/sugang/cc/cc100InterfaceExcel.action"
        const val DEFAULT_LECTURE_PARAMS =
            """seeMore=더보기&srchBdNo=&srchCamp=&srchOpenSbjtFldCd=&srchCptnCorsFg=&srchCurrPage=1&srchExcept=&srchGenrlRemoteLtYn=&srchIsEngSbjt=&srchIsPendingCourse=&srchLsnProgType=&srchMrksApprMthdChgPosbYn=&srchMrksGvMthd=&srchOpenUpDeptCd=&srchOpenMjCd=&srchOpenPntMax=&srchOpenPntMin=&srchOpenSbjtDayNm=&srchOpenSbjtNm=&srchOpenSbjtTm=&srchOpenSbjtTmNm=&srchOpenShyr=&srchOpenSubmattCorsFg=&srchOpenSubmattFgCd1=&srchOpenSubmattFgCd2=&srchOpenSubmattFgCd3=&srchOpenSubmattFgCd4=&srchOpenSubmattFgCd5=&srchOpenSubmattFgCd6=&srchOpenSubmattFgCd7=&srchOpenSubmattFgCd8=&srchOpenSubmattFgCd9=&srchOpenDeptCd=&srchOpenUpSbjtFldCd=&srchPageSize=9999&srchProfNm=&srchSbjtCd=&srchSbjtNm=&srchTlsnAplyCapaCntMax=&srchTlsnAplyCapaCntMin=&srchTlsnRcntMax=&srchTlsnRcntMin=&workType=EX"""
    }

    suspend fun getCoursebookCondition(): SugangSnuCoursebookCondition =
        sugangSnuApi.post().uri { builder ->
            builder.path(SUGANG_SNU_COURSEBOOK_PATH)
                .query(DEFAULT_COURSEBOOK_PARAMS)
                .build()
        }.awaitExchange { it.awaitBody() }

    suspend fun getSugangSnuLectures(
        year: Int,
        semester: Semester,
        lectureCategory: LectureCategory = LectureCategory.NONE,
        language: String = "ko",
    ): PooledDataBuffer =
        sugangSnuApi.get().uri { builder ->
            builder.run {
                path(SUGANG_SNU_LECTURE_PATH)
                query(DEFAULT_LECTURE_PARAMS)
                queryParam("srchLanguage", language)
                queryParam("srchOpenSchyy", year)
                queryParam("srchOpenShtm", semester.toSugangSnuSearchString())
                if (lectureCategory != LectureCategory.NONE) {
                    replaceQueryParam("srchOpenSbjtFldCd", lectureCategory.queryValue)
                    replaceQueryParam("srchOpenUpSbjtFldCd", lectureCategory.parentCategory)
                }
                build()
            }
        }
            .accept(MediaType.TEXT_HTML)
            .awaitExchange { it.awaitBody() }

    // 기존 코드에 있었으나 필요없는 것으로 판단
    // TODO: 다음학기 정상작동 시 삭제
//    suspend fun getCoursebookCondition(year: Int, semester: Semester): SugangSnuCoursebookCondition =
//        sugangSnuApi.post().uri { builder ->
//            builder.path(SUGANG_SNU_COURSEBOOK_PATH)
//                .query(DEFAULT_COURSEBOOK_PARAMS)
//                .queryParam("srchOpenSchyy", year)
//                .queryParam("srchOpenShtm", semester.toSugangSnuSearchString())
//                .build()
//        }.awaitExchange { it.awaitBody() }
}