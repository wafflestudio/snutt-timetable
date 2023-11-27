package com.wafflestudio.snu4t.notification.data

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

enum class NotificationType(
    @JsonValue
    val value: Int,
) {
    NORMAL(0),
    COURSEBOOK(1),
    LECTURE_UPDATE(2),
    LECTURE_REMOVE(3),
    LECTURE_VACANCY(4),
    FRIEND(5),
    FEATURE_NEW(6);

    companion object {
        private val valueMap = NotificationType.values().associateBy { e -> e.value }

        fun getOfValue(value: Int): NotificationType? = valueMap[value]
    }
}

@ReadingConverter
@Component
class NotificationTypeReadConverter : Converter<Int, NotificationType> {
    override fun convert(source: Int): NotificationType = NotificationType.getOfValue(source)!!
}

@Component
@WritingConverter
class NotificationTypeWriteConverter : Converter<NotificationType, Int> {
    override fun convert(source: NotificationType): Int = source.value
}
