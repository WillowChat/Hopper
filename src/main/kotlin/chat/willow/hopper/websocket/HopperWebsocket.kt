package chat.willow.hopper.websocket

import chat.willow.hopper.HopperRunner.authenticator
import chat.willow.hopper.auth.IUserTokenAuthenticator
import chat.willow.hopper.loggerFor
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.text.Charsets.UTF_8

@WebSocket
class HopperWebsocket {

    private val LOGGER = loggerFor<HopperWebsocket>()

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        val user = authenticate(session)
        if (user == null) {
            LOGGER.info("unauthenticated user tried to connect: $session")
            session.close(401, "unauthorized")
        } else {
            LOGGER.info("user $user opened a websocket connection")
        }
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, message: String) {
        val user = authenticate(session)
        if (user == null) {
            LOGGER.info("unauthenticated user tried to send a message: $session")
            session.close(401, "unauthorized")
        } else {
            LOGGER.info("user $user sent a websocket message: $message")
        }
    }

    @OnWebSocketClose
    fun onClose(session: Session, statusCode: Int, reason: String) {

    }

    private fun authenticate(session: Session): String? {
        val authValue = AuthHeaderExtractor.extract(session.upgradeRequest.headers) ?: return null
        val userToToken = authValue.split(':', limit = 2)
        val username = userToToken.getOrNull(0) ?: return null
        val token = userToToken.getOrNull(1) ?: return null

        if (username.isBlank() || token.isBlank()) {
            return null
        }

        // todo: sanitise username?

        val authed = authenticator.credentialsMatch(username, token)
        if (!authed) {
            return null
        } else {
            return username
        }
    }
}

object AuthHeaderExtractor {

    val AUTH_HEADER = "Authorization"
    val AUTH_PREFIX = "Basic "
    val MAX_LENGTH = 512

    fun extract(headers: Map<String, List<String?>>): String? {
        val headerValues = headers[AUTH_HEADER] ?: return null
        if (headerValues.size != 1) {
            return null
        }

        val headerValue = headerValues.first() ?: return null

        if (headerValue.isEmpty() || headerValue.length > MAX_LENGTH || !headerValue.startsWith(AUTH_PREFIX)) {
            return null
        }

        val encodedAuthOnly = headerValue.removePrefix(AUTH_PREFIX)
        if (encodedAuthOnly.isBlank()) {
            return null
        }

        val decodedBytes = Base64.getDecoder().decode(encodedAuthOnly)
        val decodedAuth = try {
            String(decodedBytes, UTF_8)
        } catch (e: UnsupportedEncodingException) {
            return null
        }

        if (decodedAuth.isBlank() || !decodedAuth.contains(':')) {
            return null
        }

        return decodedAuth
    }

}