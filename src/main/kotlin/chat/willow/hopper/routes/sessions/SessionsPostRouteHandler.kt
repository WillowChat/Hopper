package chat.willow.hopper.routes.sessions

import chat.willow.hopper.HopperDatabase
import chat.willow.hopper.HopperRunner
import chat.willow.hopper.loggerFor
import chat.willow.hopper.routes.JsonRouteHandler
import chat.willow.hopper.routes.RouteResult
import chat.willow.hopper.routes.shared.ErrorResponseBody
import chat.willow.hopper.routes.stringParser
import chat.willow.hopper.routes.stringSerialiser
import com.squareup.moshi.Moshi
import org.pac4j.core.profile.CommonProfile
import java.util.*

class SessionsPostRouteHandler(moshi: Moshi) : JsonRouteHandler<SessionsPostRequestBody, SessionsPostResponseBody>(moshi.stringParser(), moshi.stringSerialiser(), moshi.stringSerialiser()) {

    private val LOGGER = loggerFor<SessionsPostRouteHandler>()

    override fun handle(request: SessionsPostRequestBody, user: CommonProfile?): RouteResult<SessionsPostResponseBody, ErrorResponseBody> {
        if (request.user.isNullOrBlank() || request.password.isNullOrBlank()) {
            return RouteResult.failure(code = 400, error = ErrorResponseBody(code = 123, message = "badly formatted user or password"))

        }

        val dbUser = HopperDatabase.getUser(request.user) ?: return badCredentials
        val decodedPassword = HopperRunner.Pbdfk2HmacSha512PasswordHasher.decode(dbUser.password) ?: return badCredentials

        // todo: validate key length

        val hashedProvidedPassword = HopperRunner.Pbdfk2HmacSha512PasswordHasher.compute(request.password, salt = decodedPassword.salt, iterations = decodedPassword.iterations, keyLength = decodedPassword.hashSize * 8) ?: return badCredentials
        val base64ProvidedPassword = Base64.getEncoder().encodeToString(hashedProvidedPassword)

        if (base64ProvidedPassword != decodedPassword.encodedHash) return badCredentials

        var storedToken = false
        var newToken = ""

        val dbUserTokens = HopperDatabase.getUserTokens(dbUser.username) ?: return RouteResult.failure(code = 500, error = ErrorResponseBody(code = 123, message = "couldn't fetch user tokens"))

        while (!storedToken) {
            newToken = HopperRunner.tokenGenerator.nextSessionId()

            val alreadyExists = dbUserTokens.any { it.contains(newToken) }
            if (alreadyExists) {
                continue
            }

            HopperDatabase.addUserToken(dbUser.userid, newToken)

            storedToken = true
        }

        LOGGER.info("Stored new auth token for user ${request.user}")

        return RouteResult.success(value = SessionsPostResponseBody(token = newToken))
    }

    val badCredentials: RouteResult<SessionsPostResponseBody, ErrorResponseBody> = RouteResult.failure(code = 401, error = ErrorResponseBody(code = 123, message = "bad credentials"))

}