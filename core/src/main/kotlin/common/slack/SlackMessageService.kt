package com.wafflestudio.snu4t.common.slack

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.composition.PlainTextObject
import com.slack.api.model.block.element.BlockElements.button
import com.slack.api.model.kotlin_extension.block.dsl.LayoutBlockDsl
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private const val SNUTT_CHANNEL_ID = "C0PAVPS5T"
const val CONFIRM_ONGOING_EMOJI = "acongablob"
const val CONFIRM_DONE_EMOJI = "done"

interface SlackMessageService {

    suspend fun postMessage(channel: String = SNUTT_CHANNEL_ID, message: SlackMessageRequest): SlackMessageResponse

    suspend fun postMessageToThread(channel: String = SNUTT_CHANNEL_ID, threadTs: String, message: SlackMessageRequest): SlackMessageResponse

    suspend fun addEmoji(channel: String = SNUTT_CHANNEL_ID, threadTs: String, emoji: String)

    suspend fun deleteEmoji(channel: String = SNUTT_CHANNEL_ID, threadTs: String, emoji: String)
}

@Component
internal class Impl(
    @Value("\${slack.bot.token}") secretKey: String,
) : SlackMessageService {

    private val client = Slack.getInstance()
        .methodsAsync(secretKey)

    override suspend fun postMessage(channel: String, message: SlackMessageRequest): SlackMessageResponse {
        return client.chatPostMessage { builder ->
            builder.channel(channel)
                .text(message.text)
                .blocks { apply(message.blocks) }
        }
            .await()
            .let {
                if (!it.isOk) {
                    log.error("Failed to post message to slack: ${it.error}")
                }

                SlackMessageResponse(it.channel, it.ts)
            }
    }

    override suspend fun postMessageToThread(channel: String, threadTs: String, message: SlackMessageRequest): SlackMessageResponse {
        return client.chatPostMessage { builder ->
            builder
                .channel(channel)
                .threadTs(threadTs)
                .text(message.text)
                .blocks { apply(message.blocks) }
        }
            .await()
            .let {
                if (!it.isOk) {
                    log.error("Failed to post message to slack: ${it.error}")
                }

                SlackMessageResponse(it.channel, it.ts)
            }
    }

    override suspend fun addEmoji(channel: String, threadTs: String, emoji: String) {
        client.reactionsAdd { builder ->
            builder
                .channel(channel)
                .timestamp(threadTs)
                .name(emoji)
        }
            .await()
            .let {
                if (!it.isOk) {
                    log.error("Failed to add emoji to slack: ${it.error}")
                }
            }
    }

    override suspend fun deleteEmoji(channel: String, threadTs: String, emoji: String) {
        client.reactionsRemove { builder ->
            builder
                .channel(channel)
                .timestamp(threadTs)
                .name(emoji)
        }
            .await()
            .let {
                if (!it.isOk) {
                    log.error("Failed to delete emoji to slack: ${it.error}")
                }
            }
    }

    private fun LayoutBlockDsl.apply(blocks: List<SlackMessageBlock>) {
        blocks.forEach { block ->
            when (block) {
                is SlackMessageBlock.Header -> header {
                    text(block.text, emoji = false)
                }
                is SlackMessageBlock.Section -> section {
                    markdownText(block.text, verbatim = false)
                }
                is SlackMessageBlock.Button -> button {
                    it.text(PlainTextObject(block.text, false))
                    block.url?.let { url -> it.url(url) }
                }
                is SlackMessageBlock.Action -> actions {
                    elements {
                        button {
                            actionId(block.actionId)
                            value(block.value)
                            text(block.text)
                        }
                    }
                }
            }
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)
}
