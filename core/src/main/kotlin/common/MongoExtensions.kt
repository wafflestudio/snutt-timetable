package com.wafflestudio.snu4t.common

import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

infix fun Criteria.isEqualTo(o: Any?): Criteria = this.`is`(o)
infix fun Criteria.elemMatch(c: Criteria): Criteria = this.elemMatch(c)

fun Query.addInWhereIfNotEmpty(field: String, values: List<*>?) {
    if (!values.isNullOrEmpty()) {
        addCriteria(Criteria.where(field).`in`(values))
    }
}
