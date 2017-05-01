package chat.willow.hopper.routes.servers

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.loggerFor
import chat.willow.hopper.routes.JsonRouteHandler
import chat.willow.hopper.routes.RouteResult
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.routes.stringParser
import chat.willow.hopper.routes.stringSerialiser
import chat.willow.warren.WarrenClient
import com.squareup.moshi.Moshi
import org.pac4j.core.profile.CommonProfile
import kotlin.concurrent.thread

class ServersPostRouteHandler(moshi: Moshi) : JsonRouteHandler<ServersPostRequestBody, ServersPostResponseBody>(moshi.stringParser(), moshi.stringSerialiser(), moshi.stringSerialiser()) {

    private val LOGGER = loggerFor<ServersPostRouteHandler>()

    override fun handle(request: ServersPostRequestBody, user: CommonProfile?): RouteResult<ServersPostResponseBody, ErrorResponseBody> {
        LOGGER.info("handling POST /servers: $request")

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

        return RouteResult.success(value = ServersPostResponseBody(id = serverId))
    }

}