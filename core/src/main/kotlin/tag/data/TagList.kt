package com.wafflestudio.snu4t.tag.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.wafflestudio.snu4t.common.enum.Semester
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

@Document("taglists")
@CompoundIndex(def = "{'year': 1, 'semester': 1}", unique = true)
data class TagList(
    @Id
    @JsonProperty("_id")
    var id: String? = null,
    val year: Int,
    val semester: Semester,
    @Field("updated_at")
    @JsonProperty("updated_at")
    val updatedAt: Instant = Instant.now(),
    @Field("tags")
    @JsonProperty("tags")
    val tagCollection: TagCollection
)

data class TagCollection(
    val classification: List<String>,
    val department: List<String>,
    @Field("academic_year")
    val academicYear: List<String>,
    val credit: List<String>,
    val instructor: List<String>,
    val category: List<String>,
    val sortCriteria: List<String>
)
