package com.wafflestudio.snu4t.sugangsnu.job.sync.data

import com.wafflestudio.snu4t.lectures.data.Lecture
import kotlin.reflect.KProperty1

class UpdatedLecture(
    val oldData: Lecture,
    val newData: Lecture,
    val updatedField: List<KProperty1<Lecture, *>>
)
