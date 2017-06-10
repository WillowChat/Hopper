package chat.willow.hopper.auth

import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Pbdfk2HmacSha512PasswordStorage {

    val defaultIterations = 64000

    fun compute(plaintext: String, salt: String, iterations: Int = defaultIterations, keyLength: Int = 256): ByteArray? {
        // todo: don't assert?
        assert(plaintext.length >= 8)
        assert(salt.length >= 8)
        assert(iterations >= 10000)

        try {
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            val spec = PBEKeySpec(plaintext.toCharArray(), salt.toByteArray(charset = Charsets.UTF_8), iterations, keyLength)
            val key = skf.generateSecret(spec)

            if (key.encoded.isEmpty()) {
                return null
            }

            return key.encoded

        } catch (e: NoSuchAlgorithmException) {
            return null
        } catch (e: InvalidKeySpecException) {
            return null
        }
    }

    // algorithm:iterations:hashSize:salt:hash
    // 1:64000:hashsize:salt:hash

    fun encode(salt: String, computedHash: ByteArray, iterations: Int = defaultIterations): String {
        assert(!computedHash.isEmpty())
        assert(!salt.contains(':'))

        val encodedHash = Base64.getEncoder().encodeToString(computedHash)
        return "1:$iterations:${computedHash.size}:$salt:$encodedHash"
    }

    fun decode(encodedPassword: String): Decoded? {
        val parts = encodedPassword.split(':', limit = 5)
        if (parts.any { it.contains(":") } || parts.size != 5) {
            return null
        }

        val iterations = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val hashSize = parts.getOrNull(2)?.toIntOrNull() ?: return null
        val salt = parts.getOrNull(3) ?: return null
        val encodedHash = parts.getOrNull(4) ?: return null

        return Decoded(encodedHash, iterations, hashSize, salt)
    }

    data class Decoded(val encodedHash: String, val iterations: Int, val hashSize: Int, val salt: String)

}