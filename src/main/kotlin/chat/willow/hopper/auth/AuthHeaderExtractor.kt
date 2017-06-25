package chat.willow.hopper.auth

import java.io.UnsupportedEncodingException
import java.util.*

data class UserAndToken(val user: String, val token: String)

interface IAuthHeaderExtractor {
    fun extract(headers: Map<String, List<String?>>): UserAndToken?
    fun extract(headerValue: String): UserAndToken?
}

object AuthHeaderExtractor : IAuthHeaderExtractor {

    val AUTH_HEADER = "Authorization"
    val AUTH_PREFIX = "Basic "
    val MAX_LENGTH = 512

    override fun extract(headers: Map<String, List<String?>>): UserAndToken? {
        val headerValues = headers[AUTH_HEADER] ?: return null
        if (headerValues.size != 1) {
            return null
        }

        val headerValue = headerValues.first() ?: return null

        return extract(headerValue)
    }

    override fun extract(headerValue: String): UserAndToken? {
        if (headerValue.isEmpty() || headerValue.length > MAX_LENGTH || !headerValue.startsWith(AUTH_PREFIX)) {
            return null
        }

        val encodedAuthOnly = headerValue.removePrefix(AUTH_PREFIX)
        if (encodedAuthOnly.isBlank()) {
            return null
        }

        val decodedBytes = Base64.getDecoder().decode(encodedAuthOnly)
        val decodedAuth = try {
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: UnsupportedEncodingException) {
            return null
        }

        if (decodedAuth.isBlank() || !decodedAuth.contains(':')) {
            return null
        }

        val userToToken = decodedAuth.split(':', limit = 2)
        val username = userToToken.getOrNull(0) ?: return null
        val token = userToToken.getOrNull(1) ?: return null

        return UserAndToken(username, token)
    }
}