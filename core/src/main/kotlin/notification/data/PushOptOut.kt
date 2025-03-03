package com.wafflestudio.snutt.notification.data

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType

@Document(collection = "push_opt_out")
@CompoundIndex(def = "{ 'user_id': 1, 'push_category': 1 }", unique = true)
data class PushOptOut(
    @Id
    val id: String? = null,
    @Indexed
    @Field("user_id", targetType = FieldType.OBJECT_ID)
    val userId: String,
    @Field("push_category")
    @Indexed
    val pushCategory: PushCategory,
)
