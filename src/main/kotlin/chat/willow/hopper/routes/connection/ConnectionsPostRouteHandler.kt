package chat.willow.hopper.routes.connection

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.connections.HopperConnection
import chat.willow.hopper.connections.IHopperConnections
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.websocket.IWebSocketUserTracker
import chat.willow.hopper.websocket.WebSocketUserTracker
import chat.willow.hopper.websocket.messages.NewConnection
import chat.willow.warren.WarrenClient
import com.google.common.net.HostSpecifier
import com.google.common.net.InternetDomainName
import com.squareup.moshi.Moshi
import kotlin.concurrent.thread

data class ConnectionsPostRequestBody(val host: String, val port: Int, val tls: Boolean, val nick: String)

data class ConnectionsPostResponseBody(val connection: HopperConnection)

class ConnectionsPostRouteHandler(moshi: Moshi, private val connections: IHopperConnections, private val tracker: IWebSocketUserTracker) :
        JsonRouteHandler<ConnectionsPostRequestBody, ConnectionsPostResponseBody, AuthenticatedContext>(
                moshi.stringParser(),
                moshi.stringSerialiser(),
                moshi.stringSerialiser(),
                AuthenticatedContext.Builder
        ) {

    private val LOGGER = loggerFor<ConnectionsPostRouteHandler>()

    override fun handle(request: ConnectionsPostRequestBody, context: AuthenticatedContext): RouteResult<ConnectionsPostResponseBody, ErrorResponseBody> {
        LOGGER.info("handling POST /connections: $request")

        val hostValid = HostSpecifier.isValid(request.host)
        if (!hostValid) {
            return jsonFailure(400, "host failed validation")
        }

        val portValid = request.port in 1..65535
        if (!portValid) {
            return jsonFailure(400, "port failed validation")
        }

        // todo: validate nickname?

        val connection = connections.add(request.host, request.port, request.tls, request.nick) ?: return jsonFailure(500, "failed to create connection")

        tracker.send(NewConnection.Payload(id = connection.id), user = context.user)

        return RouteResult.success(value = ConnectionsPostResponseBody(connection))
    }

}