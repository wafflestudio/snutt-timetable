package com.wafflestudio.snu4t.middleware

import com.wafflestudio.snu4t.common.exception.WrongApiKeyException
import com.wafflestudio.snu4t.handler.RequestContext
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import javax.crypto.spec.SecretKeySpec

@Component
class ApiKeyMiddleware(
    @Value("\${snutt.secret-key}") private val secretKey: String,
) : Middleware {
    private val keySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
    private val jwtParser = Jwts.parser().setSigningKey(keySpec)
    private val stringToKeyVersionMap = mapOf(
        "ios" to "0",
        "web" to "0",
        "android" to "0",
        "test" to "0",
    )

    override suspend fun invoke(req: ServerRequest, context: RequestContext): RequestContext {
        try {
            val apiKey = requireNotNull(req.headers().firstHeader("x-access-apikey"))
            val claims = jwtParser.parseClaimsJws(apiKey).body

            val string = claims["string"]!!.toString()
            val keyVersion = claims["key_version"]!!.toString()
            require(stringToKeyVersionMap[string] == keyVersion)
        } catch (e: Exception) {
            throw WrongApiKeyException
        }

        return context
    }
}
