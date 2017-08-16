package chat.willow.hopper.routes.connection

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.connections.HopperConnection
import chat.willow.hopper.connections.HopperConnections
import chat.willow.hopper.connections.IHopperConnections
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.AuthenticatedContext
import chat.willow.hopper.routes.JsonRouteHandler
import chat.willow.hopper.routes.RouteResult
import chat.willow.hopper.routes.shared.EmptyBody
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.routes.stringSerialiser
import com.squareup.moshi.Moshi

data class ConnectionsGetResponseBody(val connections: Set<HopperConnection>)

class ConnectionsGetRouteHandler(moshi: Moshi, private val connections: IHopperConnections) :
        JsonRouteHandler<EmptyBody, ConnectionsGetResponseBody, AuthenticatedContext>(
                EmptyBody,
                moshi.stringSerialiser(),
                moshi.stringSerialiser(),
                AuthenticatedContext.Builder
        ) {

    private val LOGGER = loggerFor<ConnectionsGetRouteHandler>()

    override fun handle(request: EmptyBody, context: AuthenticatedContext): RouteResult<ConnectionsGetResponseBody, ErrorResponseBody> {
        LOGGER.info("handling GET /connection for all: $request")

        val servers = connections.all()

        return RouteResult.success(value = ConnectionsGetResponseBody(servers))
    }

}