package chat.willow.hopper.routes.connection

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.loggerFor
import chat.willow.hopper.routes.AuthenticatedContext
import chat.willow.hopper.routes.JsonRouteHandler
import chat.willow.hopper.routes.RouteResult
import chat.willow.hopper.routes.shared.EmptyBody
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.routes.stringSerialiser
import com.squareup.moshi.Moshi

data class ConnectionsGetResponseBody(val connections: Set<Server>)

data class Server(val id: String, val server: String, val nick: String)

class ConnectionsGetRouteHandler(moshi: Moshi) :
        JsonRouteHandler<EmptyBody, ConnectionsGetResponseBody, AuthenticatedContext>(
                EmptyBody,
                moshi.stringSerialiser(),
                moshi.stringSerialiser(),
                AuthenticatedContext.Builder
        ) {

    private val LOGGER = loggerFor<ConnectionsGetRouteHandler>()

    override fun handle(request: EmptyBody, context: AuthenticatedContext): RouteResult<ConnectionsGetResponseBody, ErrorResponseBody> {
        LOGGER.info("handling GET /connections: $request")

        val servers = HopperRunner.usersToServers[context.user] ?: mutableSetOf()

        return RouteResult.success(value = ConnectionsGetResponseBody(servers))
    }

}