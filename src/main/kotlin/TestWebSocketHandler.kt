import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket
class TestWebSocketHandler {

    @OnWebSocketConnect
    fun onConnect(user: Session) {
        Hopper.users[user] = Hopper.userCount.andIncrement
        Hopper.broadcast("user ${Hopper.userCount} connected")
    }

    @OnWebSocketClose
    fun onClose(user: Session, statusCode: Int, reason: String) {
        Hopper.users.remove(user)
        Hopper.broadcast("user closed: $statusCode $reason")
    }

    @OnWebSocketMessage
    fun onMessage(user: Session, message: String) {
        Hopper.broadcast("user ${Hopper.userCount} sent message: $message")
    }
}