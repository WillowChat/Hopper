package chat.willow.hopper

import chat.willow.kale.irc.CharacterCodes
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket
class TestWebSocketHandler {

    private val LOGGER = loggerFor<TestWebSocketHandler>()

    @OnWebSocketConnect
    fun onConnect(user: Session) {
        val userId = HopperRunner.userCount.andIncrement
        HopperRunner.users[user] = userId

        val message = "user $userId connected"
        LOGGER.info(message)
        HopperRunner.broadcast(message)
    }

    @OnWebSocketClose
    fun onClose(user: Session, statusCode: Int, reason: String) {
        val userId = HopperRunner.users[user]
        HopperRunner.users.remove(user)

        val message = "user $userId closed: $statusCode $reason"
        LOGGER.info(message)
        HopperRunner.broadcast(message)
    }

    @OnWebSocketMessage
    fun onMessage(user: Session, message: String) {
        val userId = HopperRunner.users[user]

        val broadcastMessage = "user $userId sent message: $message"
        LOGGER.info(broadcastMessage)
        HopperRunner.broadcast(broadcastMessage)

        if (message.startsWith("send")) {
            val parameters = message.split(CharacterCodes.SPACE, limit = 3)
            if (parameters.size < 3) {
                LOGGER.warn("user $userId tried to send something but didn't give enough parameters")
                return
            }

            val target = parameters[1]
            val targetMessage = parameters[2]

            HopperRunner.send(targetMessage, target)
        }
    }
}