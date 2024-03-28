package com.wafflestudio.snu4t.common.slack

data class SlackMessageRequest(
    val text: String = "",
    val blocks: List<SlackMessageBlock>,
) {
    constructor(vararg blocks: SlackMessageBlock, text: String = "") : this(text, blocks.toList())
}

interface SlackMessageBlock {

    data class Header(val text: String) : SlackMessageBlock

    data class Section(val text: String) : SlackMessageBlock

    data class Button(val text: String, val url: String? = null) : SlackMessageBlock

    enum class Action(
        val actionId: String,
        val value: String,
        val text: String,
    ) : SlackMessageBlock {
        SUGANG_SNU_CONFIRM("sugang_snu_confirm", "ok", "확인 완료"),
    }
}

data class SlackMessageResponse(
    val channel: String,
    val threadTs: String
)
