package chat.willow.hopper.websocket

import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.IStringSerialiser
import chat.willow.hopper.routes.stringSerialiser
import chat.willow.hopper.websocket.messages.*
import org.eclipse.jetty.websocket.api.Session
import java.util.concurrent.ConcurrentHashMap

// users should be able to open multiple websockets
//  notifications get sent to all sessions for a given user id
interface IWebSocketUserTracker {
    fun add(user: String, session: Session)
    fun remove(user: String, session: Session)
    fun onMessage(user: String, session: Session, message: String)
    fun <T: Any> send(payload: T, user: String)
}

class WebSocketUserTracker: IWebSocketUserTracker {

    private val LOGGER = loggerFor<WebSocketUserTracker>()

    // todo: concurrency - lock on user instead?
    private val usersToSessions = ConcurrentHashMap<String, MutableSet<Session>>()

    override fun add(user: String, session: Session) {
        val userSessions = usersToSessions[user] ?: mutableSetOf()

        userSessions += session

        usersToSessions[user] = userSessions
    }

    override fun remove(user: String, session: Session) {
        val userSessions = usersToSessions[user] ?: return

        userSessions -= session

        if (userSessions.isEmpty()) {
            usersToSessions.remove(user)
        } else {
            usersToSessions[user] = userSessions
        }
    }

    override fun onMessage(user: String, session: Session, message: String) {
        // pass to handler or something
        // do we even want to handle websocket requests at all?
    }

    private val payloadToOwner = mapOf<Class<*>, WebSocketTypedMessage>(
            NewConnection.Payload::class.java to NewConnection,
                ConnectionRemoved.Payload::class.java to ConnectionRemoved,
                ConnectionStarted.Payload::class.java to ConnectionStarted,
                ConnectionStopped.Payload::class.java to ConnectionStopped
    )
    private val payloadOwnerSerialisers = mapOf<Class<*>, WebSocketMessageSerialising<*>>(
            NewConnection.Payload::class.java to NewConnection.Serialiser,
            ConnectionRemoved.Payload::class.java to ConnectionRemoved.Serialiser,
            ConnectionStopped.Payload::class.java to ConnectionStopped.Serialiser,
            ConnectionStarted.Payload::class.java to ConnectionStarted.Serialiser
    )

    override fun <T: Any> send(payload: T, user: String) {
        @Suppress("UNCHECKED_CAST")
        val messageSerialiser = payloadOwnerSerialisers[payload::class.java]?.serialiser as? IStringSerialiser<WebSocketMessage<T>>
        if (messageSerialiser == null) {
            LOGGER.info("missing serialiser for payload $payload")
            return
        }

        val owner = payloadToOwner[payload::class.java]
        if (owner == null) {
            LOGGER.info("couldn't find owner for payload $payload")
            return
        }

        val messageWithPayload = WebSocketMessage(type = owner.type, payload = payload)

        val outputMessage = messageSerialiser.serialise(messageWithPayload)

        val userSessions = usersToSessions[user] ?: mutableSetOf()

        for (session in userSessions) {
            // todo: evaluate futures / nonblocking
            session.remote.sendString(outputMessage)
        }
    }
}