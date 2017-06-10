package chat.willow.hopper.auth

import spark.Filter
import spark.Request
import spark.Response
import spark.Service

class BasicAuthSparkFilter(private val authHeaderExtractor: IAuthHeaderExtractor, private val authenticator: IUserTokenAuthenticator, private val service: Service) : Filter {

    companion object {
        val ATTRIBUTE_KEY = "hopper_user"

        fun authenticatedUser(request: Request): AuthenticatedUser? {
            return request.attribute<AuthenticatedUser>(ATTRIBUTE_KEY)
        }

        fun storeAuthenticatedUser(request: Request, user: AuthenticatedUser) {
            request.attribute(ATTRIBUTE_KEY, user)
        }
    }

    data class AuthenticatedUser(val username: String)

    override fun handle(request: Request, response: Response) {
        val authHeaderValue = request.headers(AuthHeaderExtractor.AUTH_HEADER)
        if (authHeaderValue.isNullOrEmpty()) {
            return unauthorized()
        }

        val (user, token) = authHeaderExtractor.extract(authHeaderValue) ?: return unauthorized()

        val authed = authenticator.credentialsMatch(user, token)
        if (!authed) {
            return unauthorized()
        }

        storeAuthenticatedUser(request, AuthenticatedUser(user))
    }

    private fun unauthorized() {
        service.halt(401)
    }

}