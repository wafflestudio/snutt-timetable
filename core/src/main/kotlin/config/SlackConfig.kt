package com.wafflestudio.snu4t.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
import com.slack.api.bolt.App
import com.slack.api.bolt.AppConfig
import com.slack.api.bolt.request.RequestHeaders
import com.slack.api.bolt.request.builtin.BlockActionRequest
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.wafflestudio.snu4t.common.cache.Cache
import com.wafflestudio.snu4t.common.cache.CacheKey
import com.wafflestudio.snu4t.common.cache.get
import com.wafflestudio.snu4t.common.push.dto.PushMessage
import com.wafflestudio.snu4t.common.slack.CONFIRM_DONE_EMOJI
import com.wafflestudio.snu4t.common.slack.CONFIRM_ONGOING_EMOJI
import com.wafflestudio.snu4t.common.slack.SlackMessageBlock
import com.wafflestudio.snu4t.common.slack.SlackMessageRequest
import com.wafflestudio.snu4t.common.slack.SlackMessageService
import com.wafflestudio.snu4t.notification.data.NotificationType
import com.wafflestudio.snu4t.notification.service.PushWithNotificationService
import com.wafflestudio.snu4t.users.repository.UserRepository
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

const val SNUTT_ID = "S032EFLT1FT"
const val SNUTT_MENTION = "<!subteam^$SNUTT_ID>"

// bolt 가 서블릿 기반이라 webflux 호환이 안되어서 직접 지정
@RestController
@Profile("!test")
class SlackController(
    private val slackApp: App,
    private val objectMapper: ObjectMapper
) {
    @PostMapping("/slack/interactions", consumes = ["*/*"])
    suspend fun handleInteractions(
        @RequestHeader headers: Map<String, String>,
        serverWebExchange: ServerWebExchange
    ) {
        val requestHeaders = RequestHeaders(headers.mapValues { listOf(it.value) })
        val payload = serverWebExchange.formData.awaitSingle()["payload"]?.firstOrNull() ?: ""
        val slashCommandRequest = BlockActionRequest(payload, payload, requestHeaders)
        slackApp.run(slashCommandRequest)
    }

    @PostMapping("/slack/command", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun handle(
        @RequestHeader headers: Map<String, String>,
        serverWebExchange: ServerWebExchange
    ) {
        val formData = serverWebExchange.formData.awaitSingle()
        val slackCommandPayload = SlashCommandPayload().apply {
            token = formData.getFirst("token")
            teamId = formData.getFirst("team_id")
            teamDomain = formData.getFirst("team_domain")
            enterpriseId = formData.getFirst("enterprise_id")
            enterpriseName = formData.getFirst("enterprise_name")
            channelId = formData.getFirst("channel_id")
            channelName = formData.getFirst("channel_name")
            userId = formData.getFirst("user_id")
            userName = formData.getFirst("user_name")
            command = formData.getFirst("command")
            text = formData.getFirst("text")
            responseUrl = formData.getFirst("response_url")
            triggerId = formData.getFirst("trigger_id")
        }
        val requestHeaders = RequestHeaders(headers.mapValues { listOf(it.value) })
        val slashCommandRequest = SlashCommandRequest(objectMapper.writeValueAsString(formData), slackCommandPayload, requestHeaders)
        slackApp.run(slashCommandRequest)
    }
}

@Configuration
class SlackConfig(
    @Value("\${slack.bot.token}") private val secretKey: String,
    @Value("\${slack.user.token}") private val userSecretKey: String,
    @Value("\${slack.signing-secret}") private val secret: String,
    private val cache: Cache,
    private val phase: Phase,
    private val slackMessageService: SlackMessageService,
    private val userRepository: UserRepository,
    private val pushWithNotificationService: PushWithNotificationService
) {
    private val log = LoggerFactory.getLogger(SlackConfig::class.java)

    @Bean
    @Profile("!test")
    fun app(): App {
        return App(
            AppConfig.builder()
                .singleTeamBotToken(secretKey)
                .userScope(userSecretKey)
                .signingSecret(secret)
                .requestVerificationEnabled(true)
                .build()
        ).apply {
            use { req, _, chain ->
                val userGroupResponse = this.client.usergroupsUsersList {
                    it.token(userSecretKey)
                    it.usergroup(SNUTT_ID)
                }

                check(userGroupResponse.isOk && userGroupResponse.users.any { it == req.context.requestUserId }) { "권한이 없습니다." }
                chain.next(req)
            }

            blockAction(SlackMessageBlock.Action.SUGANG_SNU_CONFIRM.actionId) { request, context ->
                log.info("SUGANG_SNU_CONFIRM request from ${request.payload.user.name}")
                val button = request.payload.actions.find { it.type == "button" && it.actionId == SlackMessageBlock.Action.SUGANG_SNU_CONFIRM.actionId }
                if (button == null) {
                    log.error("SUGANG_SNU_CONFIRM request from ${request.payload.user.name} has no button")
                    return@blockAction context.ack()
                }

                runBlocking {
                    val threadTs = cache.get<String>(CacheKey.LOCK_LIVE_SUGANG_SNU_SYNC_UNTIL_CONFIRMED.build())
                    if (threadTs == null) {
                        log.error("SUGANG_SNU_CONFIRM request from ${request.payload.user.name} has no threadTs")
                        context.respond("이미 확인이 완료된 건입니다.")
                    } else {
                        cache.delete(CacheKey.LOCK_LIVE_SUGANG_SNU_SYNC_UNTIL_CONFIRMED.build())
                        slackMessageService.deleteEmoji(emoji = CONFIRM_ONGOING_EMOJI, threadTs = threadTs)
                        slackMessageService.addEmoji(emoji = CONFIRM_DONE_EMOJI, threadTs = threadTs)
                        slackMessageService.postMessageToThread(
                            threadTs = threadTs,
                            message = SlackMessageRequest(
                                SlackMessageBlock.Section("<@${request.payload.user.name}> 님께서 수강신청 동기화 결과를 확인했습니다."),
                                SlackMessageBlock.Section("다음 일자에 수강신청 동기화 결과가 반영됩니다."), // TODO("K8S API 로 직접 잡 트리거?")
                            )
                        )

                        context.respond("확인을 완료했습니다.")
                    }
                }

                context.ack()
            }

            command("/push") { req, ctx ->
                if (phase.isProd) {
                    return@command ctx.ack("프로덕션에서는 사용할 수 없습니다.") // TODO 토큰 분리
                }

                runBlocking {
                    val text = req.payload.text
                    val adminUsers = userRepository.findAllByIsAdminTrue().mapNotNull { it.id }
                    pushWithNotificationService.sendPushesAndNotifications(
                        pushMessage = PushMessage(
                            title = "${req.payload.userName}: $text",
                            body = "어드민 테스트 푸시입니다.",
                            data = mapOf("type" to "test")
                        ),
                        notificationType = NotificationType.NORMAL,
                        userIds = adminUsers
                    )
                }

                ctx.respond("푸시를 보냈습니다.")
                ctx.ack()
            }
        }
    }
}
