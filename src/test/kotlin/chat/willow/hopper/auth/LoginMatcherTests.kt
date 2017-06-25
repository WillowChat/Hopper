package chat.willow.hopper.auth

import chat.willow.hopper.db.ILoginDataSource
import chat.willow.hopper.db.UserLogin
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class LoginMatcherTests {

    private lateinit var sut: LoginMatcher
    private lateinit var mockLoginDataSource: ILoginDataSource
    private lateinit var mockPasswordStorage: IPbdfk2HmacSha512PasswordStorage

    @Before fun setUp() {
        mockLoginDataSource = mock()
        mockPasswordStorage = mock()

        sut = LoginMatcher(mockLoginDataSource, mockPasswordStorage)
    }

    @Test fun `correct user and password, derived keys match, returns user`() {
        val stubDerivedKey = "derived_key".toByteArray(charset = Charsets.UTF_8)
        val base64StubDerivedKey = Base64.getEncoder().encodeToString("derived_key".toByteArray(charset = Charsets.UTF_8))
        val authEntry = Pbdfk2HmacSha512PasswordStorage.DecodedEntry(base64StubDerivedKey, 0, 0, "")
        val stubUserLogin = UserLogin("", "", "")

        whenever(mockLoginDataSource.getUserLogin("user")).thenReturn(stubUserLogin)
        whenever(mockPasswordStorage.decode("")).thenReturn(authEntry)
        whenever(mockPasswordStorage.deriveKey(eq("password"), any(), any(), any())).thenReturn(stubDerivedKey)

        val userLogin = sut.findMatching("user", "password")

        assertEquals(stubUserLogin, userLogin)
    }

    @Test fun `no user with that username, returns null`() {
        val stubDerivedKey = "derived_key".toByteArray(charset = Charsets.UTF_8)
        val base64StubDerivedKey = Base64.getEncoder().encodeToString("derived_key".toByteArray(charset = Charsets.UTF_8))
        val authEntry = Pbdfk2HmacSha512PasswordStorage.DecodedEntry(base64StubDerivedKey, 0, 0, "")
        val stubUserLogin = UserLogin("", "", "")

        whenever(mockLoginDataSource.getUserLogin("other_user")).thenReturn(stubUserLogin)
        whenever(mockPasswordStorage.decode("")).thenReturn(authEntry)
        whenever(mockPasswordStorage.deriveKey(eq("password"), any(), any(), any())).thenReturn(stubDerivedKey)

        val userLogin = sut.findMatching("user", "password")

        assertNull(userLogin)
    }

    @Test fun `derived keys different, returns null`() {
        val stubDerivedKey = "derived_key_1".toByteArray(charset = Charsets.UTF_8)
        val base64StubDerivedKey = Base64.getEncoder().encodeToString("derived_key_2".toByteArray(charset = Charsets.UTF_8))
        val authEntry = Pbdfk2HmacSha512PasswordStorage.DecodedEntry(base64StubDerivedKey, 0, 0, "")
        val stubUserLogin = UserLogin("", "", "")

        whenever(mockLoginDataSource.getUserLogin("user")).thenReturn(stubUserLogin)
        whenever(mockPasswordStorage.decode("")).thenReturn(authEntry)
        whenever(mockPasswordStorage.deriveKey(eq("password"), any(), any(), any())).thenReturn(stubDerivedKey)

        val userLogin = sut.findMatching("user", "password")

        assertNull(userLogin)
    }

}