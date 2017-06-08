package chat.willow.hopper.routes.sessions

import chat.willow.hopper.HopperRunner
import chat.willow.hopper.auth.Pbdfk2HmacSha512PasswordStorage
import chat.willow.hopper.db.HopperDatabase
import chat.willow.hopper.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.ErrorResponseBody
import com.squareup.moshi.Moshi
import java.util.*

data class SessionsPostRequestBody(val user: String, val password: String)

data class SessionsPostResponseBody(val token: String)

class SessionsPostRouteHandler(moshi: Moshi) : JsonRouteHandler<SessionsPostRequestBody, SessionsPostResponseBody, EmptyContext>(moshi.stringParser(), moshi.stringSerialiser(), moshi.stringSerialiser(), EmptyContext.Builder) {

    private val LOGGER = loggerFor<SessionsPostRouteHandler>()

    override fun handle(request: SessionsPostRequestBody, context: EmptyContext): RouteResult<SessionsPostResponseBody, ErrorResponseBody> {
        if (request.user.isNullOrBlank() || request.password.isNullOrBlank()) {
            return RouteResult.failure(code = 400, error = ErrorResponseBody(code = 123, message = "badly formatted user or password"))

        }

        val dbUser = HopperDatabase.getUser(request.user) ?: return badCredentials
        val decodedPassword = Pbdfk2HmacSha512PasswordStorage.decode(dbUser.password) ?: return badCredentials

        // todo: validate key length

        val hashedProvidedPassword = Pbdfk2HmacSha512PasswordStorage.compute(request.password, salt = decodedPassword.salt, iterations = decodedPassword.iterations, keyLength = decodedPassword.hashSize * 8) ?: return badCredentials
        val base64ProvidedPassword = Base64.getEncoder().encodeToString(hashedProvidedPassword)

        if (base64ProvidedPassword != decodedPassword.encodedHash) return badCredentials

        val newToken = HopperRunner.tokenGenerator.nextSessionId()

        // todo: handle failure
        HopperDatabase.addUserToken(dbUser.userid, newToken)

        LOGGER.info("Stored new auth token for user ${request.user}")

        return RouteResult.success(value = SessionsPostResponseBody(token = newToken))
    }

    val badCredentials: RouteResult<SessionsPostResponseBody, ErrorResponseBody> = RouteResult.failure(code = 401, error = ErrorResponseBody(code = 123, message = "bad credentials"))

}