package chat.willow.hopper.routes.connection

import chat.willow.hopper.connections.HopperConnection
import chat.willow.hopper.connections.IHopperConnections
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.EmptyBody
import chat.willow.hopper.routes.shared.ErrorResponseBody
import com.squareup.moshi.Moshi
import spark.Request

// todo: compose contexts
data class ConnectionGetContext(val authenticatedContext: AuthenticatedContext, val id: String) {

    companion object Builder: IContextBuilder<ConnectionGetContext> {
        override fun build(request: Request): ConnectionGetContext? {
            val authContext = AuthenticatedContext.Builder.build(request) ?: return null
            val id = request.params("id") ?: return null

            return ConnectionGetContext(authContext, id)
        }
    }

}

data class ConnectionGetResponseBody(val connection: HopperConnection)

class ConnectionGetRouteHandler(moshi: Moshi, private val connections: IHopperConnections) :
        JsonRouteHandler<EmptyBody, ConnectionGetResponseBody, ConnectionGetContext>(
                EmptyBody,
                moshi.stringSerialiser(),
                moshi.stringSerialiser(),
                ConnectionGetContext.Builder
        ) {

    private val LOGGER = loggerFor<ConnectionGetRouteHandler>()

    override fun handle(request: EmptyBody, context: ConnectionGetContext): RouteResult<ConnectionGetResponseBody, ErrorResponseBody> {
        LOGGER.info("handling GET /connection for id ${context.id}: $request")

        val server = connections[context.id] ?: return jsonFailure(404, message = "couldn't find a server with id ${context.id}")

        return RouteResult.success(value = ConnectionGetResponseBody(server))
    }

}