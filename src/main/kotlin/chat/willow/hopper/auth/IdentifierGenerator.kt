package chat.willow.hopper.auth

import java.math.BigInteger
import java.security.SecureRandom

interface IIdentifierGenerator {
    fun next(): String
}

class IdentifierGenerator(val bits: Int = 260): IIdentifierGenerator {
    private val random = SecureRandom()

    override fun next(): String {
        return BigInteger(bits, random).toString(32)
    }
}