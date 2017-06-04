package chat.willow.hopper.auth

import chat.willow.hopper.db.HopperDatabase
import org.pac4j.core.context.Pac4jConstants
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.exception.CredentialsException
import org.pac4j.core.exception.HttpAction
import org.pac4j.core.profile.CommonProfile

interface IUserTokenAuthenticator {
    fun credentialsMatch(username: String, token: String): Boolean
}

class UserTokenAuthenticator: IUserTokenAuthenticator {

    override fun credentialsMatch(username: String, token: String): Boolean {
        val dbUserTokens = HopperDatabase.getUserTokens(username) ?: return false
        return dbUserTokens.contains(token)
    }

}

class Pac4JUserTokenAuthenticator(val authenticator: IUserTokenAuthenticator) : Authenticator<UsernamePasswordCredentials> {

    @Throws(HttpAction::class, CredentialsException::class)
    override fun validate(credentials: UsernamePasswordCredentials?, context: WebContext) {
        if (credentials == null) {
            throw HttpAction.unauthorized("no credentials", context, "hopper")
        }

        if (credentials.username.isNullOrBlank() || credentials.password.isNullOrBlank()) {
            throw HttpAction.unauthorized("malformed credentials", context, "hopper")
        }

        val authed = authenticator.credentialsMatch(credentials.username, credentials.password)
        if (!authed) {
            throw CredentialsException("unauthorized")
        }

        val profile = CommonProfile()
        profile.setId(credentials.username)
        profile.addAttribute(Pac4jConstants.USERNAME, credentials.username)

        credentials.userProfile = profile
    }

}