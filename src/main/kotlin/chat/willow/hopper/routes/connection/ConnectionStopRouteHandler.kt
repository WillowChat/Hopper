package chat.willow.hopper.routes.connection

import chat.willow.hopper.connections.IHopperConnections
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.EmptyBody
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.websocket.IWebSocketUserTracker
import chat.willow.hopper.websocket.messages.ConnectionStopped
import com.squareup.moshi.Moshi
import spark.Request

data class ConnectionStopContext(val authenticatedContext: AuthenticatedContext, val id: String) {

    companion object Builder: IContextBuilder<ConnectionStopContext> {
        override fun build(request: Request): ConnectionStopContext? {
            val authContext = AuthenticatedContext.Builder.build(request) ?: return null
            val id = request.params("connection_id") ?: return null

            return ConnectionStopContext(authContext, id)
        }
    }

}

class ConnectionStopRouteHandler(moshi: Moshi, private val connections: IHopperConnections, private val tracker: IWebSocketUserTracker) :
        JsonRouteHandler<EmptyBody, EmptyBody, ConnectionStopContext>(
                EmptyBody,
                EmptyBody,
                moshi.stringSerialiser(),
                ConnectionStopContext.Builder
        ) {

    private val LOGGER = loggerFor<ConnectionStopRouteHandler>()

    override fun handle(request: EmptyBody, context: ConnectionStopContext): RouteResult<EmptyBody, ErrorResponseBody> {
        LOGGER.info("handling GET /connection/<id>/stop: $request")

        // todo: sanity check id
        val server = connections[context.id] ?: return jsonFailure(404, message = "couldn't find a server with id ${context.id}")
        connections.stop(context.id)

        tracker.send(ConnectionStopped.Payload(id = context.id), user = context.authenticatedContext.user)

        return RouteResult.success(value = EmptyBody)
    }

}