package chat.willow.hopper.websocket

import chat.willow.hopper.auth.IAuthHeaderExtractor
import chat.willow.hopper.auth.IUserTokenAuthenticator
import chat.willow.hopper.logging.loggerFor
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket
class HopperWebSocket(private val authenticator: IUserTokenAuthenticator,
                      private val authHeaderExtractor: IAuthHeaderExtractor,
                      private val tracker: IWebSocketUserTracker) {

    private val LOGGER = loggerFor<HopperWebSocket>()

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        val user = authenticate(session)
        if (user == null) {
            LOGGER.info("unauthenticated user tried to connect: $session")
            session.close(401, "unauthorized")
        } else {
            LOGGER.info("user $user opened a websocket connection")
            tracker.add(user, session)
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
            tracker.onMessage(user, session, message)
        }
    }

    @OnWebSocketClose
    fun onClose(session: Session, statusCode: Int, reason: String) {
        val user = authenticate(session)
        if (user == null) {
            LOGGER.info("unauthenticated user closed socket?: $session")
            session.close(401, "unauthorized")
        } else {
            LOGGER.info("user $user closed a websocket connection")
            tracker.remove(user, session)
        }
    }

    private fun authenticate(session: Session): String? {
        val (user, token) = authHeaderExtractor.extract(session.upgradeRequest.headers) ?: return null

        val authed = authenticator.credentialsMatch(user, token)
        if (!authed) {
            return null
        } else {
            return user
        }
    }
}