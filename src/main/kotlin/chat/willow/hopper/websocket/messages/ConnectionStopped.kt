package chat.willow.hopper.websocket.messages

import chat.willow.hopper.routes.stringSerialiser

object ConnectionStopped : WebSocketTypedMessage {
    override val type = "connection_stopped"

    data class Payload(val id: String)

    object Serialiser: WebSocketMessageSerialising<Payload> {
        override val serialiser = WEBSOCKET_PAYLOAD_MOSHI.stringSerialiser<WebSocketMessage<Payload>>(WebSocketMessage::class.java, Payload::class.java)
    }
}