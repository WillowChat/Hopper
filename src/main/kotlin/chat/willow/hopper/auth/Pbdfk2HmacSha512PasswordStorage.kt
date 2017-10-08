package chat.willow.hopper.auth

import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

typealias EncodedEntry = String

interface IPbdfk2HmacSha512PasswordStorage {

    fun deriveKey(plaintext: String,
                  salt: String,
                  iterations: Int = Pbdfk2HmacSha512PasswordStorage.DEFAULT_ITERATIONS,
                  keyLengthBits: Int = Pbdfk2HmacSha512PasswordStorage.DEFAULT_KEY_LENGTH_BITS): ByteArray?

    fun encode(salt: String,
               computedHash: ByteArray,
               iterations: Int = Pbdfk2HmacSha512PasswordStorage.DEFAULT_ITERATIONS): EncodedEntry?

    fun decode(encodedEntry: EncodedEntry): Pbdfk2HmacSha512PasswordStorage.DecodedEntry?

}

object Pbdfk2HmacSha512PasswordStorage: IPbdfk2HmacSha512PasswordStorage {

    val DEFAULT_ITERATIONS = 64000
    val DEFAULT_KEY_LENGTH_BITS = 256

    private val BITS_IN_A_BYTE = 8

    private val MIN_ITERATIONS = 10000
    private val MAX_ITERATIONS = 1000000
    private val MIN_SALT_LENGTH = 8
    private val MAX_SALT_LENGTH = 256
    private val MIN_PLAINTEXT_LENGTH = 8
    private val MAX_PLAINTEXT_LENGTH = 256

    private val MIN_ENCODED_HASH_SANITY = 8

    override fun deriveKey(plaintext: String, salt: String, iterations: Int, keyLengthBits: Int): ByteArray? {
        if (plaintext.length < MIN_PLAINTEXT_LENGTH || plaintext.length > MAX_PLAINTEXT_LENGTH) {
            return null
        }

        if (salt.length < MIN_SALT_LENGTH || salt.length > MAX_SALT_LENGTH) {
            return null
        }

        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            return null
        }

        try {
            // TODO: Extract and make testable by wrapping SKF stuff

            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            val spec = PBEKeySpec(plaintext.toCharArray(), salt.toByteArray(charset = Charsets.UTF_8), iterations, keyLengthBits)
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
    // 1:64000:hashsizebits:salt:hash

    override fun encode(salt: String, computedHash: ByteArray, iterations: Int): String? {
        if (computedHash.isEmpty()) {
            return null
        }

        if (salt.contains(':')) {
            return null
        }

        val encodedHash = Base64.getEncoder().encodeToString(computedHash)
        return "1:$iterations:${computedHash.size * BITS_IN_A_BYTE}:$salt:$encodedHash"
    }

    override fun decode(encodedEntry: String): DecodedEntry? {
        val parts = encodedEntry.split(':', limit = 5)
        if (parts.any { it.contains(":") } || parts.size != 5) {
            return null
        }

        val iterations = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val hashSize = parts.getOrNull(2)?.toIntOrNull() ?: return null
        val salt = parts.getOrNull(3) ?: return null
        val encodedHash = parts.getOrNull(4) ?: return null

        if (salt.length < MIN_SALT_LENGTH || salt.length > MAX_SALT_LENGTH) {
            return null
        }

        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            return null
        }

        if (encodedHash.length < MIN_ENCODED_HASH_SANITY) {
            return null
        }

        return DecodedEntry(encodedHash, iterations, hashSize, salt)
    }

    private fun extractFrom(parts: List<String>, index: Int): Int? {
        val part: String = parts.getOrNull(index) ?: return null
        return part.toIntOrNull()
    }

    data class DecodedEntry(val derivedKeyHash: String, val iterations: Int, val hashSize: Int, val salt: String)

}