package chat.willow.hopper

import chat.willow.hopper.auth.*
import chat.willow.hopper.connections.HopperConnections
import chat.willow.hopper.connections.IHopperConnections
import chat.willow.hopper.db.HopperDatabase
import chat.willow.hopper.db.ITokenDataSink
import chat.willow.hopper.logging.loggerFor
import chat.willow.hopper.routes.connection.*
import chat.willow.hopper.routes.session.SessionsPostRouteHandler
import chat.willow.hopper.websocket.HopperWebSocket
import chat.willow.hopper.websocket.IWebSocketUserTracker
import chat.willow.hopper.websocket.WebSocketUserTracker
import chat.willow.warren.IWarrenClient
import com.squareup.moshi.Moshi
import spark.Service
import java.util.*

object HopperRunner {

    private val LOGGER = loggerFor<HopperRunner>()

    var users: MutableMap<org.eclipse.jetty.websocket.api.Session, Int> = mutableMapOf()

    val saltGenerator = IdentifierGenerator(bits = 256)
    val userIdGenerator = IdentifierGenerator(bits = 130)

    val moshi = Moshi.Builder().build()

    private var warren: IWarrenClient? = null

    @JvmStatic fun main(args: Array<String>) {
        LOGGER.info("Support the development of this bouncer at https://crrt.io/patreon 🐰🎉")
        LOGGER.info("Starting up...")

        HopperDatabase.doMigrations()
        doFirstTimeUsageIfNecessary()

        val authenticator = UserTokenAuthenticator(HopperDatabase)
        val authHeaderExtractor = AuthHeaderExtractor

        val tokenGenerator = IdentifierGenerator(bits = 260)
        val loginMatcher = LoginMatcher(HopperDatabase, Pbdfk2HmacSha512PasswordStorage)

        val serverIdGenerator = IdentifierGenerator(bits = 130)
        val connections = HopperConnections(serverIdGenerator)

        val webSocketUserTracker = WebSocketUserTracker()

        val allUsers = HopperDatabase.getUsers()

        for (user in allUsers) {
            LOGGER.info("Loading connections for ${user.user}")

            val userConnections = HopperDatabase.getUserConnections(user.user)
            if (userConnections == null) {
                LOGGER.error(" Failed to get connections from database, skipping user")
                continue
            }

            LOGGER.info(" Found ${userConnections.size} to load")

            userConnections.forEach {
                LOGGER.info(" Tracking: $it")
                connections.track(it)
            }

            LOGGER.info(" Loaded!")
        }

        HopperWebService(authHeaderExtractor, authenticator, loginMatcher, HopperDatabase, tokenGenerator, connections, webSocketUserTracker).start()
    }

    fun doFirstTimeUsageIfNecessary() {
        val numberOfUsers = HopperDatabase.computeNumberOfUsers() ?: throw RuntimeException("Failed to get number of users from the database - migrations failed?")

        if (numberOfUsers > 0) {
            LOGGER.info("Not doing first-time setup as at least one user already exists")
            return
        }

        val scanner = Scanner(System.`in`)
        System.out.println("First time setup!")
        System.out.println("Please enter a username (letters and digits only, < 32 characters):")

        val newUsername = scanner.nextLine()
        val userId = userIdGenerator.next()

        if (newUsername.isNullOrBlank() || !newUsername.all { it.isLetterOrDigit() || newUsername.length > 32 }) {
            throw RuntimeException("Username can only be letters and digits, relaunch and try again")
        }

        // TODO: zxcvbn - warn if crappy
        System.out.println("Now enter a password for that user ([8..64] characters):")
        val userPassword = scanner.nextLine()

        if (userPassword.isNullOrBlank() || userPassword.length < 8 || userPassword.length > 64) {
            throw RuntimeException("Password must be within [8..64] characters")
        }

        val salt = saltGenerator.next()
        val derivedKey = Pbdfk2HmacSha512PasswordStorage.deriveKey(userPassword, salt) ?: throw RuntimeException("Couldn't derive key from password")

        val encodedPassword = Pbdfk2HmacSha512PasswordStorage.encode(salt, derivedKey) ?: throw RuntimeException("Couldn't encode derived key")

        LOGGER.info("Adding new user $userId")
        HopperDatabase.addNewUser(userId, newUsername, encodedPassword)
    }

    fun broadcast(message: String) {
        synchronized(HopperRunner) {
            users.keys
                    .filter { it.isOpen }
                    .forEach { it.remote.sendString(message) }
        }
    }

    fun send(message: String, channel: String) {
        val warren = warren ?: return

        warren.send(message, channel)
    }

}

class HopperWebService(private val authHeaderExtractor: IAuthHeaderExtractor,
                       private val authenticator: IUserTokenAuthenticator,
                       private val loginMatcher: ILoginMatcher,
                       private val tokenDataSink: ITokenDataSink,
                       private val tokenGenerator: IIdentifierGenerator,
                       private val connections: IHopperConnections,
                       private val webSocketUserTracker: IWebSocketUserTracker) {

    fun start() {
        val http = Service.ignite()

        http.webSocket("/websocket", HopperWebSocket(authenticator, authHeaderExtractor, webSocketUserTracker))

        http.path("/session") {
            http.post("", SessionsPostRouteHandler(HopperRunner.moshi, loginMatcher, tokenDataSink, tokenGenerator))
        }

        http.path("/v1") {
            http.before("/*", BasicAuthSparkFilter(authHeaderExtractor, authenticator, http))

            http.path("/connection") {
                http.get("", ConnectionsGetRouteHandler(HopperRunner.moshi, connections))
                http.post("", ConnectionsPostRouteHandler(HopperRunner.moshi, connections, webSocketUserTracker, HopperDatabase))

                http.path("/:connection_id") {
                    http.get("", ConnectionGetRouteHandler(HopperRunner.moshi, connections))
                    http.delete("", ConnectionDeleteRouteHandler(HopperRunner.moshi, connections, webSocketUserTracker))
                    http.get("/start", ConnectionStartRouteHandler(HopperRunner.moshi, connections, webSocketUserTracker))
                    http.get("/stop", ConnectionStopRouteHandler(HopperRunner.moshi, connections, webSocketUserTracker))
                }
            }
        }
    }

}

