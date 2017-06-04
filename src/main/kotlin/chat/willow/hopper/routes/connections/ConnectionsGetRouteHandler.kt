package chat.willow.hopper.routes.connections

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.auth.BasicSparkAuthFilter
import chat.willow.hopper.loggerFor
import chat.willow.hopper.routes.JsonRouteHandler
import chat.willow.hopper.routes.RouteResult
import chat.willow.hopper.routes.shared.EmptyBody
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.routes.stringSerialiser
import com.squareup.moshi.Moshi

class ConnectionsGetRouteHandler(moshi: Moshi) : JsonRouteHandler<EmptyBody, ConnectionsGetResponseBody>(EmptyBody, moshi.stringSerialiser(), moshi.stringSerialiser()) {

    private val LOGGER = loggerFor<ConnectionsGetRouteHandler>()

    override fun handle(request: EmptyBody, user: BasicSparkAuthFilter.AuthenticatedUser?): RouteResult<ConnectionsGetResponseBody, ErrorResponseBody> {
        if (user == null) {
            return unauthenticatedError()
        }

        LOGGER.info("handling GET /connections: $request")

        val servers = HopperRunner.usersToServers[user.username] ?: mutableSetOf()

        return RouteResult.success(value = ConnectionsGetResponseBody(servers))
    }

}