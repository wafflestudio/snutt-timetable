package com.wafflestudio.snutt.notification.data

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

enum class PushCategory(
    @JsonValue val value: Int,
) {
    NORMAL(0),
    LECTURE_UPDATE(1),
    VACANCY_NOTIFICATION(2),
    ;

    companion object {
        private val valueMap = PushCategory.entries.associateBy { e -> e.value }

        fun getOfValue(value: Int): PushCategory? = valueMap[value]
    }
}

@ReadingConverter
@Component
class PushCategoryReadConverter : Converter<Int, PushCategory> {
    override fun convert(source: Int): PushCategory = PushCategory.getOfValue(source)!!
}

@WritingConverter
@Component
class PushCategoryWriteConverter : Converter<PushCategory, Int> {
    override fun convert(source: PushCategory): Int = source.value
}

fun PushCategory(notificationType: NotificationType) =
    when (notificationType) {
        NotificationType.LECTURE_UPDATE -> PushCategory.LECTURE_UPDATE
        NotificationType.LECTURE_VACANCY -> PushCategory.VACANCY_NOTIFICATION
        else -> PushCategory.NORMAL
    }
