package chat.willow.hopper.auth

import java.math.BigInteger
import java.security.SecureRandom

class IdentifierGenerator(val bits: Int = 260) {
    private val random = SecureRandom()

    fun nextSessionId(): String {
        return BigInteger(bits, random).toString(32)
    }
}