package chat.willow.hopper.auth

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthHeaderExtractorTests {

    private lateinit var sut: AuthHeaderExtractor

    private val userTokenBase64 = "dXNlcjp0b2tlbg==" // user:token
    private val wellFormedAuthHeaderValue = "Basic $userTokenBase64"

    private val userTokenWithColonBase64 = "dXNlcjp0Om9rZW4=" // user:t:oken
    private val userOnlyBase64 = "dXNlcm9ubHk=" // user

    @Before fun setUp() {
        sut = AuthHeaderExtractor
    }

    @Test fun `user and token are successfully extracted from a single, well-formed auth header`() {
        val userAndToken = sut.extract(wellFormedAuthHeaderValue)

        assertEquals(UserAndToken("user", "token"), userAndToken)
    }

    @Test fun `missing auth header returns null`() {
        val userAndToken = sut.extract(mapOf("Header-Key" to listOf("Header-Value")))

        assertNull(userAndToken)
    }

    @Test fun `multiple auth headers returns null`() {
        val userAndToken = sut.extract(mapOf("Authorization" to listOf(wellFormedAuthHeaderValue, wellFormedAuthHeaderValue)))

        assertNull(userAndToken)
    }

    @Test fun `auth header blank returns null`() {
        val userAndToken = sut.extract(mapOf("Authorization" to listOf("")))

        assertNull(userAndToken)
    }

    @Test fun `auth header missing basic prefix returns null`() {
        val userAndToken = sut.extract(mapOf("Authorization" to listOf(userTokenBase64)))

        assertNull(userAndToken)
    }

    @Test fun `auth header base64 malformed returns null`() {
        val userAndToken = sut.extract(mapOf("Authorization" to listOf("garbage$wellFormedAuthHeaderValue")))

        assertNull(userAndToken)
    }

    @Test fun `user token has a colon in it still succeeds and contains colon`() {
        val userAndToken = sut.extract("Basic $userTokenWithColonBase64")

        assertEquals(UserAndToken("user", "t:oken"), userAndToken)
    }

    @Test fun `auth header value only has user in it returns null`() {
        val userAndToken = sut.extract("Basic $userOnlyBase64")

        assertNull(userAndToken)
    }

}