package com.wafflestudio.snu4t.users.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.snu4t.users.data.Credential
import com.wafflestudio.snu4t.users.data.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

interface AuthService {
    fun isValidLocalId(localId: String): Boolean

    fun isValidPassword(password: String): Boolean

    fun isValidEmail(email: String): Boolean

    fun isMatchedPassword(user: User, password: String): Boolean

    fun generateCredentialHash(credential: Credential): String

    fun buildLocalCredential(localId: String, password: String): Credential
}

@Service
class AuthServiceImpl(
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper,
    @Value("\${snutt.secret-key}") private val secretKey: String,
) : AuthService {
    companion object {
        private val localIdRegex = """^[a-zA-Z0-9]{4,32}$""".toRegex()
        private val passwordRegex = """^(?=.*\d)(?=.*[a-zA-Z])\S{6,20}$""".toRegex()
        private val emailRegex = """^[a-zA-Z0-9]([-_.]?[a-zA-Z0-9])*@[a-zA-Z0-9]([-_.]?[a-zA-Z0-9])*.[a-zA-Z]{2,3}$""".toRegex()
    }

    override fun isValidLocalId(localId: String) = localId.matches(localIdRegex)

    override fun isValidPassword(password: String) = password.matches(passwordRegex)

    override fun isValidEmail(email: String) = email.matches(emailRegex)

    override fun isMatchedPassword(user: User, password: String): Boolean {
        return passwordEncoder.matches(password, user.credential.localPw)
    }

    override fun generateCredentialHash(credential: Credential): String {
        val credentialString = objectMapper.writeValueAsString(credential)
        return hmacSHA256Hex(credentialString)
    }

    override fun buildLocalCredential(localId: String, password: String) = Credential(
        localId = localId,
        localPw = passwordEncoder.encode(password),
    )

    private fun hmacSHA256Hex(data: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secretKey.toByteArray(), algorithm))
        val macResult = mac.doFinal(data.toByteArray())
        return macResult.joinToString("") { "%02x".format(it) } // base16 encoding
    }
}
