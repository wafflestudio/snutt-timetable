package com.wafflestudio.snu4t.common.mail

import kotlinx.coroutines.future.await
import org.springframework.stereotype.Component
import software.amazon.awssdk.regions.Region.AP_NORTHEAST_2
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest

@Component
class MailClient {
    private val sesClient = SesAsyncClient.builder().region(AP_NORTHEAST_2).build()
    val sourceEmail = "snutt@wafflestudio.com"

    suspend fun sendMail(
        to: String,
        subject: String,
        body: String,
    ) {
        val dest = Destination.builder().toAddresses(to).build()
        val message = Message.builder()
            .subject(Content.builder().data(subject).build())
            .body(Body.builder().html(Content.builder().data(body).build()).build())
            .build()
        val request = SendEmailRequest.builder()
            .destination(dest)
            .message(message)
            .source(sourceEmail)
            .build()
        sesClient.sendEmail(request).await()
    }
}
