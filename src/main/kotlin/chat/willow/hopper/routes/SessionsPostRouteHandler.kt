package chat.willow.hopper.routes

import chat.willow.hopper.Hopper
import chat.willow.hopper.loggerFor
import chat.willow.hopper.model.rest.ErrorResponseBody
import chat.willow.hopper.model.rest.SessionsPostRequestBody
import chat.willow.hopper.model.rest.SessionsPostResponseBody
import chat.willow.hopper.usersToTokens
import com.squareup.moshi.Moshi
import org.pac4j.core.profile.CommonProfile

class SessionsPostRouteHandler(moshi: Moshi) : JsonRouteHandler<SessionsPostRequestBody, SessionsPostResponseBody>(moshi.stringParser(), moshi.stringSerialiser(), moshi.stringSerialiser()) {

    private val LOGGER = loggerFor<SessionsPostRouteHandler>()

    override fun handle(request: SessionsPostRequestBody, user: CommonProfile?): RouteResult<SessionsPostResponseBody, ErrorResponseBody> {
        if (request.user.isNullOrBlank() || request.password.isNullOrBlank()) {
            return RouteResult.failure(code = 400, error = ErrorResponseBody(code = 123, message = "badly formatted user or password"))

        }

        val authed = Hopper.usersToPasswords[request.user]?.equals(request.password) ?: false
        if (!authed) {
            return RouteResult.failure(code = 401, error = ErrorResponseBody(code = 123, message = "bad credentials"))

        }

        var storedToken = false
        var newToken = ""

        while (!storedToken) {
            newToken = Hopper.tokenGenerator.nextSessionId()

            val alreadyExists = usersToTokens.any { it.value.contains(newToken) }
            if (alreadyExists) {
                continue
            }

            val currentTokens = usersToTokens[request.user] ?: mutableSetOf()
            currentTokens += newToken

            usersToTokens[request.user] = currentTokens
            storedToken = true
        }

        LOGGER.info("Stored new auth token for user ${request.user}")

        return RouteResult.success(value = SessionsPostResponseBody(token = newToken))
    }

}