package chat.willow.hopper.routes.connection

import chat.willow.hopper.connections.HopperConnection
import chat.willow.hopper.connections.IHopperConnections
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.EmptyBody
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.websocket.IWebSocketUserTracker
import chat.willow.hopper.websocket.messages.ConnectionRemoved
import chat.willow.hopper.websocket.messages.ConnectionStarted
import com.squareup.moshi.Moshi
import spark.Request

// todo: compose contexts
data class ConnectionDeleteContext(val authenticatedContext: AuthenticatedContext, val id: String) {

    companion object Builder: IContextBuilder<ConnectionDeleteContext> {
        override fun build(request: Request): ConnectionDeleteContext? {
            val authContext = AuthenticatedContext.Builder.build(request) ?: return null
            val id = request.params("id") ?: return null

            return ConnectionDeleteContext(authContext, id)
        }
    }

}

class ConnectionDeleteRouteHandler(moshi: Moshi, private val connections: IHopperConnections, private val tracker: IWebSocketUserTracker) :
        JsonRouteHandler<EmptyBody, EmptyBody, ConnectionDeleteContext>(
                EmptyBody,
                EmptyBody,
                moshi.stringSerialiser(),
                ConnectionDeleteContext.Builder
        ) {

    private val LOGGER = loggerFor<ConnectionGetRouteHandler>()

    override fun handle(request: EmptyBody, context: ConnectionDeleteContext): RouteResult<EmptyBody, ErrorResponseBody> {
        LOGGER.info("handling DELETE /connection for id ${context.id}: $request")

        if (context.id !in connections) {
            return jsonFailure(404, message = "couldn't find a server with id ${context.id}")
        } else {
            connections -= context.id

            tracker.send(ConnectionRemoved.Payload(id = context.id), user = context.authenticatedContext.user)

            return jsonSuccess(EmptyBody)
        }
    }

}