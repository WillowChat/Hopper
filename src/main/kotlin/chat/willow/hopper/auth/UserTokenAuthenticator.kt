package chat.willow.hopper.auth

import chat.willow.hopper.db.HopperDatabase

interface IUserTokenAuthenticator {
    fun credentialsMatch(username: String, token: String): Boolean
}

class UserTokenAuthenticator: IUserTokenAuthenticator {

    override fun credentialsMatch(username: String, token: String): Boolean {
        val dbUserTokens = HopperDatabase.getUserTokens(username) ?: return false
        return dbUserTokens.contains(token)
    }

}