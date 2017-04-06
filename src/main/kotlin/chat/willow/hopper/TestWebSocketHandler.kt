package chat.willow.hopper

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket
class TestWebSocketHandler {

    @OnWebSocketConnect
    fun onConnect(user: Session) {
        val userId = Hopper.userCount.andIncrement
        Hopper.users[user] = userId
        Hopper.broadcast("user $userId connected")
    }

    @OnWebSocketClose
    fun onClose(user: Session, statusCode: Int, reason: String) {
        val userId = Hopper.users[user]
        Hopper.users.remove(user)
        Hopper.broadcast("user $userId closed: $statusCode $reason")
    }

    @OnWebSocketMessage
    fun onMessage(user: Session, message: String) {
        val userId = Hopper.users[user]
        Hopper.broadcast("user $userId sent message: $message")
    }
}