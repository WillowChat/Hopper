package chat.willow.hopper.routes

import chat.willow.hopper.Hopper
import chat.willow.hopper.loggerFor
import chat.willow.hopper.model.rest.EmptyBody
import chat.willow.hopper.model.rest.ErrorResponseBody
import chat.willow.hopper.model.rest.ServersGetResponseBody
import com.squareup.moshi.Moshi
import org.pac4j.core.profile.CommonProfile

class ServersGetRouteHandler(moshi: Moshi) : JsonRouteHandler<EmptyBody, ServersGetResponseBody>(EmptyBody, moshi.stringSerialiser(), moshi.stringSerialiser()) {

    private val LOGGER = loggerFor<ServersGetRouteHandler>()

    override fun handle(request: EmptyBody, user: CommonProfile?): RouteResult<ServersGetResponseBody, ErrorResponseBody> {
        if (user == null) {
            return unauthenticatedError()
        }

        LOGGER.info("handling GET /servers: $request")

        val servers = Hopper.usersToServers[user.username] ?: mutableSetOf()

        return RouteResult.success(value = ServersGetResponseBody(servers))
    }

}