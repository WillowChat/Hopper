package chat.willow.hopper.routes.connections

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.auth.BasicSparkAuthFilter
import chat.willow.hopper.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.warren.WarrenClient
import com.squareup.moshi.Moshi
import kotlin.concurrent.thread

data class ConnectionsPostRequestBody(val server: String, val nick: String)

data class ConnectionsPostResponseBody(val id: String)

class ConnectionsPostRouteHandler(moshi: Moshi) : JsonRouteHandler<ConnectionsPostRequestBody, ConnectionsPostResponseBody, AuthenticatedContext>(moshi.stringParser(), moshi.stringSerialiser(), moshi.stringSerialiser(), AuthenticatedContext.Builder) {

    private val LOGGER = loggerFor<ConnectionsPostRouteHandler>()

    override fun handle(request: ConnectionsPostRequestBody, context: AuthenticatedContext): RouteResult<ConnectionsPostResponseBody, ErrorResponseBody> {
        LOGGER.info("handling POST /connections: $request")

        val serverId = HopperRunner.serverIdGenerator.nextSessionId()
        val warren = WarrenClient.build {
            server(request.server)
            user(request.nick)
        }

        warren.events.onAny {
            HopperRunner.broadcast("warren event $serverId: $it")
        }

        thread {
            LOGGER.info("warren starting $serverId")
            warren.start()
            LOGGER.info("warren ended $serverId")
        }

        HopperRunner.serversToWarrens[serverId] = warren

        val server = Server(id = serverId, server = request.server, nick = request.nick)

        var currentServers = HopperRunner.usersToServers[context.user] ?: mutableSetOf()
        currentServers += server

        HopperRunner.usersToServers[context.user] = currentServers

        return RouteResult.success(value = ConnectionsPostResponseBody(id = serverId))
    }

}