package chat.willow.hopper.auth

import chat.willow.hopper.db.ILoginDataSource
import chat.willow.hopper.db.UserLogin
import java.util.*

interface ILoginMatcher {
    fun findMatching(testUser: String, testPassword: String): UserLogin?
}

class LoginMatcher(private val loginDataSource: ILoginDataSource): ILoginMatcher {

    override fun findMatching(testUser: String, testPassword: String): UserLogin? {
        val dbUser = loginDataSource.getUserLogin(testUser) ?: return null
        val dbStoredPassword = Pbdfk2HmacSha512PasswordStorage.decode(dbUser.password) ?: return null

        // todo: validate key length
        // todo: make *8 clearer in keylength
        val hashedProvidedPassword = Pbdfk2HmacSha512PasswordStorage.compute(testPassword, salt = dbStoredPassword.salt, iterations = dbStoredPassword.iterations, keyLength = dbStoredPassword.hashSize * 8) ?: return null
        val base64ProvidedPassword = Base64.getEncoder().encodeToString(hashedProvidedPassword)

        // todo: sanity check provided and stored hashes

        if (base64ProvidedPassword != dbStoredPassword.encodedHash) {
            return null
        } else {
            return dbUser
        }
    }
}