package com.wafflestudio.snu4t.mail.service

import com.wafflestudio.snu4t.common.exception.InvalidEmailException
import com.wafflestudio.snu4t.common.mail.MailClient
import com.wafflestudio.snu4t.mail.data.MailContent
import com.wafflestudio.snu4t.mail.data.UserMailType
import com.wafflestudio.snu4t.users.service.AuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

interface MailService {
    suspend fun sendUserMail(type: UserMailType, to: String, code: String = "", localId: String = "")
}

@Service
class MailServiceImpl(
    @Value("userMailTemplate.txt") private val mailTemplateResource: ClassPathResource,
    private val mailClient: MailClient,
    private val authService: AuthService,
) : MailService {
    override suspend fun sendUserMail(type: UserMailType, to: String, code: String, localId: String) {
        if (!authService.isValidEmail(to)) {
            throw InvalidEmailException
        }
        getUserMailContent(type, to, code, localId).let { (subject, body) ->
            mailClient.sendMail(to, subject, body)
        }
    }
    private suspend fun getUserMailContent(type: UserMailType, email: String, code: String, localId: String): MailContent {
        val mailContentList = mailTemplateResource.inputStream
            .readBytes()
            .decodeToString()
            .replace("{code}", code)
            .replace("{email}", email)
            .replace("{localId}", localId)
            .split("\n\n")
            .windowed(2, 2, false)
            .map { (subject, body) -> MailContent(subject, body) }
        val mailContent = mailContentList[type.index]
        return mailContent
    }
}
