package chat.willow.hopper.routes.session

import chat.willow.hopper.auth.IIdentifierGenerator
import chat.willow.hopper.auth.ILoginMatcher
import chat.willow.hopper.db.ITokenDataSink
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.ErrorResponseBody
import com.squareup.moshi.Moshi

data class SessionsPostRequestBody(val user: String, val password: String)

data class SessionsPostResponseBody(val token: String)

class SessionsPostRouteHandler(moshi: Moshi,
                               private val loginMatcher: ILoginMatcher,
                               private val tokenDataSink: ITokenDataSink,
                               private val tokenGenerator: IIdentifierGenerator) :
        JsonRouteHandler<SessionsPostRequestBody, SessionsPostResponseBody, EmptyContext>(
                moshi.stringParser(),
                moshi.stringSerialiser(),
                moshi.stringSerialiser(),
                EmptyContext.Builder
        ) {

    private val LOGGER = loggerFor<SessionsPostRouteHandler>()

    override fun handle(request: SessionsPostRequestBody, context: EmptyContext): RouteResult<SessionsPostResponseBody, ErrorResponseBody> {
        if (request.user.isNullOrEmpty() || request.password.isNullOrEmpty()) {
            return Responses.badlyFormatted
        }

        val userLogin = loginMatcher.findMatching(request.user, request.password) ?: return Responses.badCredentials

        val newToken = storeNewUserToken(userLogin.userId) ?: return Responses.serverError

        LOGGER.info("Stored new auth token for user ${request.user}")

        return RouteResult.success(value = SessionsPostResponseBody(token = newToken))
    }

    private fun storeNewUserToken(userId: String): String? {
        val newToken = tokenGenerator.next()

        val addedToken = tokenDataSink.addUserToken(userId, newToken)
        if (!addedToken) {
            return null
        } else {
            return newToken
        }
    }

    object Responses {
        val badlyFormatted = jsonFailure<SessionsPostResponseBody>(400, "badly formatted user or password")
        val badCredentials = jsonFailure<SessionsPostResponseBody>(401, "credentials didn't match")
        val serverError = jsonFailure<SessionsPostResponseBody>(500, "server error")
    }

}