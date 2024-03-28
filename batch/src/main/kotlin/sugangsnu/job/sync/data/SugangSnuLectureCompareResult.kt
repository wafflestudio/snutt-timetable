package com.wafflestudio.snu4t.sugangsnu.job.sync.data

import com.wafflestudio.snu4t.lectures.data.Lecture

class SugangSnuLectureCompareResult(
    val createdLectureList: List<Lecture>,
    val deletedLectureList: List<Lecture>,
    val updatedLectureList: List<UpdatedLecture>,
) {
    fun needsConfirmOnProduction(): Boolean {
        return createdLectureList.size > 50 || deletedLectureList.size > 10 || updatedLectureList.size > 50
    }
}
