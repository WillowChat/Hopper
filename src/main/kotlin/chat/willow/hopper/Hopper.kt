package chat.willow.hopper

import chat.willow.hopper.auth.IdentifierGenerator
import chat.willow.hopper.auth.Pac4JConfigFactory
import chat.willow.hopper.model.rest.Server
import chat.willow.hopper.routes.ServersGetRouteHandler
import chat.willow.hopper.routes.ServersPostRouteHandler
import chat.willow.hopper.routes.SessionsPostRouteHandler
import chat.willow.warren.IWarrenClient
import com.squareup.moshi.Moshi
import org.eclipse.jetty.websocket.api.Session
import org.pac4j.sparkjava.SecurityFilter
import spark.Spark.*
import java.util.concurrent.atomic.AtomicInteger

val usersToTokens = mutableMapOf<String, MutableSet<String>>()

object Hopper {

    private val LOGGER = loggerFor<Hopper>()

    var userCount = AtomicInteger(-1)
    var users: MutableMap<Session, Int> = mutableMapOf()

    val pac4jConfig = Pac4JConfigFactory().build()
    val securityFilter = SecurityFilter(pac4jConfig, "DirectBasicAuthClient")

    val moshi = Moshi.Builder().build()
    val usersToPasswords = mutableMapOf("carrot" to "password")
    val usersToServers = mutableMapOf<String, Set<Server>>()
    val serversToWarrens = mutableMapOf<String, IWarrenClient?>()

    val tokenGenerator = IdentifierGenerator(bits = 260)
    val serverIdGenerator = IdentifierGenerator(bits = 130)

    private var warren: IWarrenClient? = null

    @JvmStatic fun main(args: Array<String>) {
        println("hello world")

        webSocket("/websocket", TestWebSocketHandler::class.java)

        path("/sessions") {
            post("", SessionsPostRouteHandler(moshi))
        }

        path("/v1") {
            before("/*", securityFilter)

            path("/servers") {
                get("", ServersGetRouteHandler(moshi))
                post("", ServersPostRouteHandler(moshi))
            }

        }

    }

    fun broadcast(message: String) {
        synchronized(Hopper) {
            users.keys
                    .filter { it.isOpen }
                    .forEach { it.remote.sendString(message) }
        }
    }

    fun send(message: String, channel: String) {
        val warren = warren ?: return

        warren.send(message, channel)
    }

}