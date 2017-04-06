package chat.willow.hopper

import org.eclipse.jetty.websocket.api.Session
import spark.Spark.*
import java.util.concurrent.atomic.AtomicInteger

object Hopper {

    var userCount = AtomicInteger(-1)
    var users: MutableMap<Session, Int> = mutableMapOf()

    @JvmStatic fun main(args: Array<String>) {
        println("hello world")

        webSocket("/websocket", TestWebSocketHandler::class.java)

        get("/hello") { _, _ -> "Hello World" }
    }

    fun broadcast(message: String) {
        users.keys
                .filter { it.isOpen }
                .forEach { it.remote.sendString(message) }
    }

}