package chat.willow.hopper.websocket.messages

import chat.willow.hopper.routes.IStringSerialiser
import com.squareup.moshi.Moshi

interface WebSocketTypedMessage {
    val type: String
}

interface WebSocketMessageSerialising<in T> {
    val serialiser: IStringSerialiser<WebSocketMessage<T>>
}

data class WebSocketMessage<out T>(val type: String, val payload: T)

val WEBSOCKET_PAYLOAD_MOSHI: Moshi = Moshi.Builder().build()