package com.wafflestudio.snu4t.users.data

import org.springframework.data.mongodb.core.mapping.Field

data class Credential(
    @Field(write = Field.Write.ALWAYS)
    var localId: String? = null,
    @Field(write = Field.Write.ALWAYS)
    var localPw: String? = null,
    @Field(write = Field.Write.ALWAYS)
    var fbId: String? = null,
    @Field(write = Field.Write.ALWAYS)
    var fbName: String? = null,
    @Field(write = Field.Write.ALWAYS)
    var appleSub: String? = null,
    @Field(write = Field.Write.ALWAYS)
    var appleEmail: String? = null,
    var appleTransferSub: String? = null,
    @Field(write = Field.Write.ALWAYS)
    var tempDate: String? = null,
    @Field(write = Field.Write.ALWAYS)
    var tempSeed: String? = null,
)
