package chat.willow.hopper.websocket.messages

import chat.willow.hopper.routes.stringSerialiser

object NewConnection: WebSocketTypedMessage {
    override val type = "new_connection"

    data class Payload(val id: String)

    object Serialiser: WebSocketMessageSerialising<Payload> {
        override val serialiser = WEBSOCKET_PAYLOAD_MOSHI.stringSerialiser<WebSocketMessage<Payload>>(WebSocketMessage::class.java, Payload::class.java)
    }
}