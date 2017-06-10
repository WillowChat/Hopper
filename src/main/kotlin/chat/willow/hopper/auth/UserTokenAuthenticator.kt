package chat.willow.hopper.auth

import chat.willow.hopper.db.ITokensDataSource

interface IUserTokenAuthenticator {
    fun credentialsMatch(username: String, token: String): Boolean
}

class UserTokenAuthenticator(private val tokensDataSource: ITokensDataSource): IUserTokenAuthenticator {

    override fun credentialsMatch(username: String, token: String): Boolean {
        val dbUserTokens = tokensDataSource.getUserTokens(username) ?: return false
        return dbUserTokens.contains(token)
    }

}