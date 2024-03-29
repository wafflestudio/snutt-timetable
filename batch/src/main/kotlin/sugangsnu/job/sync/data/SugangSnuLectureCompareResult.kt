package com.wafflestudio.snu4t.sugangsnu.job.sync.data

import com.wafflestudio.snu4t.lectures.data.Lecture

class SugangSnuLectureCompareResult(
    val createdLectureList: List<Lecture>,
    val deletedLectureList: List<Lecture>,
    val updatedLectureList: List<UpdatedLecture>,
)
