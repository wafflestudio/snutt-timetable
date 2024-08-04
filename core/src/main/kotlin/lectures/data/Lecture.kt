package com.wafflestudio.snu4t.lectures.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.wafflestudio.snu4t.common.enum.Semester
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document("lectures")
@CompoundIndex(def = "{ 'year': 1, 'semester': 1 }")
@CompoundIndex(def = "{ 'course_number': 1, 'lecture_number': 1 }")
data class Lecture(
    @Id
    @JsonProperty("_id")
    var id: String? = null,
    @Field("academic_year")
    var academicYear: String?,
    var category: String?,
    @Field("class_time_json")
    var classPlaceAndTimes: List<ClassPlaceAndTime>,
    var classification: String?,
    var credit: Long,
    var department: String?,
    var instructor: String?,
    @Field("lecture_number")
    var lectureNumber: String,
    var quota: Int,
    var freshmanQuota: Int? = null,
    var remark: String?,
    var semester: Semester,
    var year: Int,
    @Field("course_number")
    var courseNumber: String,
    @Field("course_title")
    var courseTitle: String,
    var registrationCount: Int = 0,
    var wasFull: Boolean = false,
    val evInfo: EvInfo? = null,
) {
    infix fun equalsMetadata(other: Lecture): Boolean {
        return this === other ||
            academicYear == other.academicYear &&
            category == other.category &&
            classPlaceAndTimes == other.classPlaceAndTimes &&
            classification == other.classification &&
            credit == other.credit &&
            department == other.department &&
            instructor == other.instructor &&
            lectureNumber == other.lectureNumber &&
            quota == other.quota &&
            freshmanQuota == other.freshmanQuota &&
            remark == other.remark &&
            semester == other.semester &&
            year == other.year &&
            courseNumber == other.courseNumber &&
            courseTitle == other.courseTitle
    }
}
