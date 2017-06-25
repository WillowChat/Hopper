package chat.willow.hopper.auth

import chat.willow.hopper.db.ILoginDataSource
import chat.willow.hopper.db.UserLogin
import java.util.*

interface ILoginMatcher {
    fun findMatching(testUser: String, testPassword: String): UserLogin?
}

class LoginMatcher(private val loginDataSource: ILoginDataSource, private val passwordStorage: IPbdfk2HmacSha512PasswordStorage): ILoginMatcher {

    override fun findMatching(testUser: String, testPassword: String): UserLogin? {
        val dbUser = loginDataSource.getUserLogin(testUser) ?: return null
        val dbStoredAuthEntry = passwordStorage.decode(dbUser.encodedAuthEntry) ?: return null

        val derivedKeyFromProvidedPassword = passwordStorage.deriveKey(
                testPassword,
                salt = dbStoredAuthEntry.salt,
                iterations = dbStoredAuthEntry.iterations,
                keyLengthBits = dbStoredAuthEntry.hashSize) ?: return null

        val base64DerivedKeyFromProvidedPassword = Base64.getEncoder().encodeToString(derivedKeyFromProvidedPassword)

        if (base64DerivedKeyFromProvidedPassword.isEmpty()) {
            return null
        }

        if (base64DerivedKeyFromProvidedPassword != dbStoredAuthEntry.derivedKeyHash) {
            return null
        } else {
            return dbUser
        }
    }

}