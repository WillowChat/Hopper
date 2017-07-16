package chat.willow.hopper.routes.connection

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.connections.HopperConnection
import chat.willow.hopper.connections.IHopperConnections
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.EmptyBody
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.warren.WarrenClient
import com.google.common.net.HostSpecifier
import com.google.common.net.InternetDomainName
import com.squareup.moshi.Moshi
import spark.Request
import kotlin.concurrent.thread

data class ConnectionStartContext(val authenticatedContext: AuthenticatedContext, val id: String) {

    companion object Builder: IContextBuilder<ConnectionStartContext> {
        override fun build(request: Request): ConnectionStartContext? {
            val authContext = AuthenticatedContext.Builder.build(request) ?: return null
            val id = request.params("id") ?: return null

            return ConnectionStartContext(authContext, id)
        }
    }

}

class ConnectionStartRouteHandler(moshi: Moshi, private val connections: IHopperConnections) :
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
        connections.start(context.id)

        return RouteResult.success(value = EmptyBody)
    }

}