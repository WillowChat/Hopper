package chat.willow.hopper.routes.session

import chat.willow.hopper.auth.IdentifierGenerator
import chat.willow.hopper.auth.Pbdfk2HmacSha512PasswordStorage
import chat.willow.hopper.db.ILoginDataSource
import chat.willow.hopper.db.ITokenDataSink
import chat.willow.hopper.db.UserLogin
import chat.willow.hopper.loggerFor
import chat.willow.hopper.routes.*
import chat.willow.hopper.routes.shared.ErrorResponseBody
import com.squareup.moshi.Moshi
import java.util.*

data class SessionsPostRequestBody(val user: String, val password: String)

data class SessionsPostResponseBody(val token: String)

class SessionsPostRouteHandler(moshi: Moshi,
                               private val loginDataSource: ILoginDataSource,
                               private val tokenDataSink: ITokenDataSink,
                               private val tokenGenerator: IdentifierGenerator) :
        JsonRouteHandler<SessionsPostRequestBody, SessionsPostResponseBody, EmptyContext>(
                moshi.stringParser(),
                moshi.stringSerialiser(),
                moshi.stringSerialiser(),
                EmptyContext.Builder
        ) {

    private val LOGGER = loggerFor<SessionsPostRouteHandler>()

    override fun handle(request: SessionsPostRequestBody, context: EmptyContext): RouteResult<SessionsPostResponseBody, ErrorResponseBody> {
        if (request.user.isNullOrEmpty() || request.password.isNullOrEmpty()) {
            return badlyFormatted
        }

        val userLogin = testPasswordAndFindUserLogin(request.user, request.password) ?: return badCredentials

        val newToken = storeNewUserToken(userLogin.userId) ?: return serverError

        LOGGER.info("Stored new auth token for user ${request.user}")

        return RouteResult.success(value = SessionsPostResponseBody(token = newToken))
    }

    private fun testPasswordAndFindUserLogin(user: String, testPassword: String): UserLogin? {
        val dbUser = loginDataSource.getUserLogin(user) ?: return null
        val dbStoredPassword = Pbdfk2HmacSha512PasswordStorage.decode(dbUser.password) ?: return null

        // todo: validate key length
        // todo: make *8 clearer in keylength
        val hashedProvidedPassword = Pbdfk2HmacSha512PasswordStorage.compute(testPassword, salt = dbStoredPassword.salt, iterations = dbStoredPassword.iterations, keyLength = dbStoredPassword.hashSize * 8) ?: return null
        val base64ProvidedPassword = Base64.getEncoder().encodeToString(hashedProvidedPassword)

        if (base64ProvidedPassword != dbStoredPassword.encodedHash) {
            return null
        } else {
            return dbUser
        }
    }

    private fun storeNewUserToken(userId: String): String? {
        val newToken = tokenGenerator.next()
        // todo: handle collisions?

        // todo: handle failure
        val addedToken = tokenDataSink.addUserToken(userId, newToken)
        if (!addedToken) {
            return null
        } else {
            return newToken
        }
    }

    companion object {
        val badlyFormatted: RouteResult<SessionsPostResponseBody, ErrorResponseBody> = RouteResult.failure(code = 400, error = ErrorResponseBody(code = 123, message = "badly formatted user or password"))
        val badCredentials: RouteResult<SessionsPostResponseBody, ErrorResponseBody> = RouteResult.failure(code = 401, error = ErrorResponseBody(code = 123, message = "bad credentials"))
        val serverError: RouteResult<SessionsPostResponseBody, ErrorResponseBody> = RouteResult.failure(code = 500, error = ErrorResponseBody(code = 123, message = "server error"))
    }

}