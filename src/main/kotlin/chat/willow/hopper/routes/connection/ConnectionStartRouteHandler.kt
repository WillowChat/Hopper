package chat.willow.hopper.routes.connection

import chat.willow.hopper.connections.IHopperConnections
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.EmptyBody
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.websocket.IWebSocketUserTracker
import chat.willow.hopper.websocket.messages.ConnectionStarted
import com.squareup.moshi.Moshi
import spark.Request

data class ConnectionStartContext(val authenticatedContext: AuthenticatedContext, val id: String) {

    companion object Builder: IContextBuilder<ConnectionStartContext> {
        override fun build(request: Request): ConnectionStartContext? {
            val authContext = AuthenticatedContext.Builder.build(request) ?: return null
            val id = request.params("connection_id") ?: return null

            return ConnectionStartContext(authContext, id)
        }
    }

}

class ConnectionStartRouteHandler(moshi: Moshi, private val connections: IHopperConnections, private val tracker: IWebSocketUserTracker) :
        JsonRouteHandler<EmptyBody, EmptyBody, ConnectionStartContext>(
                EmptyBody,
                EmptyBody,
                moshi.stringSerialiser(),
                ConnectionStartContext.Builder
        ) {

    private val LOGGER = loggerFor<ConnectionStartRouteHandler>()

    override fun handle(request: EmptyBody, context: ConnectionStartContext): RouteResult<EmptyBody, ErrorResponseBody> {
        LOGGER.info("handling GET /connection/<id>/start: $request")

        // todo: sanity check id
        val server = connections[context.id] ?: return jsonFailure(404, message = "couldn't find a server with id ${context.id}")
        connections.start(context.id)

        tracker.send(ConnectionStarted.Payload(id = context.id), user = context.authenticatedContext.user)

        return RouteResult.success(value = EmptyBody)
    }

}