package chat.willow.hopper.websocket

import org.eclipse.jetty.websocket.api.Session
import java.util.concurrent.ConcurrentHashMap

// users should be able to open multiple websockets
//  notifications get sent to all sessions for a given user id
interface IWebSocketUserTracker {
    fun add(user: String, session: Session)
    fun remove(user: String, session: Session)
    fun onMessage(user: String, session: Session, message: String)
    fun send(message: String, user: String)
}

class WebSocketUserTracker: IWebSocketUserTracker {

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

    override fun send(message: String, user: String) {
        val userSessions = usersToSessions[user] ?: mutableSetOf()

        for (session in userSessions) {
            // todo: evaluate futures / nonblocking
            session.remote.sendString(message)
        }
    }
}