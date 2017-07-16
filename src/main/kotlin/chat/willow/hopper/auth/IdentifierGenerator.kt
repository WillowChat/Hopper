package chat.willow.hopper.auth

import java.math.BigInteger
import java.security.SecureRandom

fun nextUniqueId(generator: IIdentifierGenerator, map: Map<*, *>): String {
    // todo: investigate if bailout or sanity assert is necessary
    while(true) {
        val id = generator.next()
        if (!map.containsKey(id)) {
            return id
        }
    }
}

interface IIdentifierGenerator {
    fun next(): String
}

class IdentifierGenerator(val bits: Int = 260): IIdentifierGenerator {
    private val random = SecureRandom()

    override fun next(): String {
        return BigInteger(bits, random).toString(32)
    }
}