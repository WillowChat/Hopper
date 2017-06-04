package chat.willow.hopper.routes.connections

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.auth.BasicSparkAuthFilter
import chat.willow.hopper.loggerFor
import chat.willow.hopper.routes.JsonRouteHandler
import chat.willow.hopper.routes.RouteResult
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.routes.stringParser
import chat.willow.hopper.routes.stringSerialiser
import chat.willow.warren.WarrenClient
import com.squareup.moshi.Moshi
import kotlin.concurrent.thread

class ConnectionsPostRouteHandler(moshi: Moshi) : JsonRouteHandler<ConnectionsPostRequestBody, ConnectionsPostResponseBody>(moshi.stringParser(), moshi.stringSerialiser(), moshi.stringSerialiser()) {

    private val LOGGER = loggerFor<ConnectionsPostRouteHandler>()

    override fun handle(request: ConnectionsPostRequestBody, user: BasicSparkAuthFilter.AuthenticatedUser?): RouteResult<ConnectionsPostResponseBody, ErrorResponseBody> {
        LOGGER.info("handling POST /connections: $request")

        if (user == null) {
            return unauthenticatedError()
        }

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

        var currentServers = HopperRunner.usersToServers[user.username] ?: mutableSetOf()
        currentServers += server

        HopperRunner.usersToServers[user.username] = currentServers

        return RouteResult.success(value = ConnectionsPostResponseBody(id = serverId))
    }

}