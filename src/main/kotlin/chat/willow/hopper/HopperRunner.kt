package chat.willow.hopper

import chat.willow.hopper.auth.*
import chat.willow.hopper.db.HopperDatabase
import chat.willow.hopper.db.HopperDatabase.database
import chat.willow.hopper.routes.connection.ConnectionsGetRouteHandler
import chat.willow.hopper.routes.connection.ConnectionsPostRouteHandler
import chat.willow.hopper.routes.connection.Server
import chat.willow.hopper.routes.session.SessionsPostRouteHandler
import chat.willow.hopper.websocket.HopperWebsocket
import chat.willow.warren.IWarrenClient
import com.squareup.moshi.Moshi
import org.jetbrains.exposed.sql.name
import spark.Service
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object HopperRunner {

    private val LOGGER = loggerFor<HopperRunner>()

    var userCount = AtomicInteger(-1)
    var users: MutableMap<org.eclipse.jetty.websocket.api.Session, Int> = mutableMapOf()

    val authenticator = UserTokenAuthenticator()
    val authHeaderExtractor = AuthHeaderExtractor

    val moshi = Moshi.Builder().build()
    val usersToServers = mutableMapOf<String, Set<Server>>()
    val serversToWarrens = mutableMapOf<String, IWarrenClient?>()

    val tokenGenerator = IdentifierGenerator(bits = 260)
    val serverIdGenerator = IdentifierGenerator(bits = 130)
    val userIdGenerator = IdentifierGenerator(bits = 130)

    val saltGenerator = IdentifierGenerator(bits = 256)

    private var warren: IWarrenClient? = null

    @JvmStatic fun main(args: Array<String>) {
        LOGGER.info("Support the development of this bouncer at https://crrt.io/patreon 🎉")
        LOGGER.info("Starting up...")

        doFirstTimeUsageIfNecessary()

        HopperWebService(authHeaderExtractor, authenticator).start()
    }

    fun doFirstTimeUsageIfNecessary() {
        // Check for presence of database
        // if it exists, do nothing
        // if it doesn't, prompt for user and password, then initialise database with it

        val databaseExists = File("./hopper.db").let { it.isFile && it.exists() }
        if (databaseExists) {
            LOGGER.info("not doing first time user setup as hopper.db already exists")
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
        val computedHashBytes = Pbdfk2HmacSha512PasswordStorage.compute(userPassword, salt) ?: throw RuntimeException("Couldn't compute password hash")

        val encodedPassword = Pbdfk2HmacSha512PasswordStorage.encode(salt, computedHashBytes)

        LOGGER.info("database name: ${database.name}")

        HopperDatabase.makeNewDatabase()
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

class HopperWebService(private val authHeaderExtractor: IAuthHeaderExtractor, private val authenticator: IUserTokenAuthenticator) {

    fun start() {
        val service = Service.ignite()

        service.webSocket("/websocket", HopperWebsocket(authenticator, authHeaderExtractor))

        service.path("/session") {
            service.post("", SessionsPostRouteHandler(HopperRunner.moshi, HopperDatabase, HopperDatabase, HopperRunner.tokenGenerator))
        }

        service.path("/v1") {
            service.before("/*", BasicSparkAuthFilter(authHeaderExtractor, authenticator, service))

            service.path("/connection") {
                service.get("", ConnectionsGetRouteHandler(HopperRunner.moshi))
                service.post("", ConnectionsPostRouteHandler(HopperRunner.moshi))

//              /connection/1
//              /connection/1/start
//              /connection/1/stop

            }
        }
    }

}

