package chat.willow.hopper.websocket.messages

import com.squareup.moshi.Moshi

interface WebSocketTypedMessage {
    val type: String
}

data class WebSocketMessage<out T>(val type: String, val payload: T)

val WEBSOCKET_PAYLOAD_MOSHI: Moshi = Moshi.Builder().build()