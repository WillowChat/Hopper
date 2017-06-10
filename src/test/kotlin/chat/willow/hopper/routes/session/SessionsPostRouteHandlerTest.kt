package chat.willow.hopper.routes.session

import chat.willow.hopper.auth.IIdentifierGenerator
import chat.willow.hopper.auth.ILoginMatcher
import chat.willow.hopper.auth.IdentifierGenerator
import chat.willow.hopper.db.ITokenDataSink
import chat.willow.hopper.db.UserLogin
import chat.willow.hopper.routes.EmptyContext
import chat.willow.hopper.routes.RouteResult
import chat.willow.hopper.routes.shared.ErrorResponseBody
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionsPostRouteHandlerTest {

    private lateinit var sut: SessionsPostRouteHandler
    private val moshi = Moshi.Builder().build()
    private lateinit var loginMatcher: ILoginMatcher
    private lateinit var tokenDataSink: ITokenDataSink
    private lateinit var tokenGenerator: IIdentifierGenerator

    private val saltGenerator = IdentifierGenerator(bits = 256) // todo: use factory so it's the same as runner

    @Before fun setUp() {
        // todo: don't use moshi in tests

        loginMatcher = mock()
        tokenDataSink = mock()
        tokenGenerator = mock()

        sut = SessionsPostRouteHandler(moshi, loginMatcher, tokenDataSink, tokenGenerator)
    }

    @Test fun `blank user or password results in 401`() {
        val responseBlankUser = sut.handle(SessionsPostRequestBody(user = "", password = "something"), EmptyContext)
        val responseBlankPassword = sut.handle(SessionsPostRequestBody(user = "someone", password = ""), EmptyContext)
        val responseBothBlank = sut.handle(SessionsPostRequestBody(user = "", password = ""), EmptyContext)

        assertEquals(SessionsPostRouteHandler.Responses.badlyFormatted, responseBlankUser)
        assertEquals(SessionsPostRouteHandler.Responses.badlyFormatted, responseBlankPassword)
        assertEquals(SessionsPostRouteHandler.Responses.badlyFormatted, responseBothBlank)
    }

    @Test fun `correct user and password result in a new token in data sink`() {
        whenever(loginMatcher.findMatching("someone", testPassword = "something")).thenReturn(UserLogin(userId = "1", user = "someone", password = ""))
        whenever(tokenDataSink.addUserToken("1", "token")).thenReturn(true)
        whenever(tokenGenerator.next()).thenReturn("token")

        val response = sut.handle(SessionsPostRequestBody(user = "someone", password = "something"), EmptyContext)

        assertEquals(RouteResult.success<SessionsPostResponseBody, ErrorResponseBody>(value = SessionsPostResponseBody(token = "token")), response)
    }

    @Test fun `incorrect user results in 401`() {
        whenever(loginMatcher.findMatching("someone", testPassword = "something")).thenReturn(UserLogin(userId = "1", user = "someone", password = ""))

        val response = sut.handle(SessionsPostRequestBody(user = "someone_else", password = "something"), EmptyContext)

        assertEquals(SessionsPostRouteHandler.Responses.badCredentials, response)
    }

    @Test fun `incorrect password results in 401`() {
        whenever(loginMatcher.findMatching("someone", testPassword = "something_else")).thenReturn(UserLogin(userId = "1", user = "someone", password = ""))

        val response = sut.handle(SessionsPostRequestBody(user = "someone", password = "not_something_else"), EmptyContext)

        assertEquals(SessionsPostRouteHandler.Responses.badCredentials, response)
    }

}