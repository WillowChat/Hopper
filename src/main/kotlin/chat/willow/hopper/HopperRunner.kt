package chat.willow.hopper

import chat.willow.hopper.HopperDatabase.database
import chat.willow.hopper.auth.IdentifierGenerator
import chat.willow.hopper.auth.Pac4JConfigFactory
import chat.willow.hopper.routes.servers.Server
import chat.willow.hopper.routes.servers.ServersGetRouteHandler
import chat.willow.hopper.routes.servers.ServersPostRouteHandler
import chat.willow.hopper.routes.sessions.SessionsPostRouteHandler
import chat.willow.warren.IWarrenClient
import com.squareup.moshi.Moshi
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.pac4j.sparkjava.SecurityFilter
import spark.Spark.*
import java.io.File
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.sql.Connection
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.text.Charsets.UTF_8

object HopperRunner {

    private val LOGGER = loggerFor<HopperRunner>()

    var userCount = AtomicInteger(-1)
    var users: MutableMap<org.eclipse.jetty.websocket.api.Session, Int> = mutableMapOf()

    val pac4jConfig = Pac4JConfigFactory().build()
    val securityFilter = SecurityFilter(pac4jConfig, "DirectBasicAuthClient")

    val moshi = Moshi.Builder().build()
    val usersToServers = mutableMapOf<String, Set<Server>>()
    val serversToWarrens = mutableMapOf<String, IWarrenClient?>()

    val tokenGenerator = IdentifierGenerator(bits = 260)
    val serverIdGenerator = IdentifierGenerator(bits = 130)
    val userIdGenerator = IdentifierGenerator(bits = 130)

    val saltGenerator = IdentifierGenerator(bits = 256)

    private var warren: IWarrenClient? = null

    @JvmStatic fun main(args: Array<String>) {
        doFirstTimeUsageIfNecessary()

        println("hello world")

        webSocket("/websocket", TestWebSocketHandler::class.java)

        path("/sessions") {
            post("", SessionsPostRouteHandler(moshi))
        }

        path("/v1") {
            before("/*", securityFilter)

            path("/servers") {
                get("", ServersGetRouteHandler(moshi))
                post("", ServersPostRouteHandler(moshi))
            }

        }

    }

    object Pbdfk2HmacSha512PasswordHasher {

        val defaultIterations = 64000

        fun compute(plaintext: String, salt: String, iterations: Int = defaultIterations, keyLength: Int = 256): ByteArray? {
            assert(plaintext.length >= 8)
            assert(salt.length >= 8)
            assert(iterations >= 10000)

            try {
                val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
                val spec = PBEKeySpec(plaintext.toCharArray(), salt.toByteArray(charset = UTF_8), iterations, keyLength)
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

    object Logins: IntIdTable() {
        val userid = varchar("userid", length = 32).uniqueIndex()
        val username = varchar("username", length = 32).uniqueIndex()
        val password = varchar("password", length = 256)
    }

    class Login(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Login>(Logins)

        var userid by Logins.userid
        var username by Logins.username
        var password by Logins.password
    }

    object Sessions: IntIdTable() {
        val userid = varchar("userid", length = 32)
        val token = varchar("token", length = 32)
    }

    class Session(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Session>(Sessions)

        var userid by Sessions.userid
        var token by Sessions.token
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
        val userId = userIdGenerator.nextSessionId()

        if (newUsername.isNullOrBlank() || !newUsername.all { it.isLetterOrDigit() || newUsername.length > 32 }) {
            throw RuntimeException("Username can only be letters and digits, relaunch and try again")
        }

        // TODO: zxcvbn - warn if crappy
        System.out.println("Now enter a password for that user ([8..64] characters):")
        val userPassword = scanner.nextLine()

        if (userPassword.isNullOrBlank() || userPassword.length < 8 || userPassword.length > 64) {
            throw RuntimeException("Password must be within [8..64] characters")
        }

        val salt = saltGenerator.nextSessionId()
        val computedHashBytes = Pbdfk2HmacSha512PasswordHasher.compute(userPassword, salt) ?: throw RuntimeException("Couldn't compute password hash")

        val encodedPassword = Pbdfk2HmacSha512PasswordHasher.encode(salt, computedHashBytes)

        LOGGER.info("database name: ${database.name}")

        HopperDatabase.makeNewDatabase()
        HopperDatabase.addNewUser(userId, newUsername, encodedPassword)

        LOGGER.info("")
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

object HopperDatabase {
    private val LOGGER = loggerFor<HopperDatabase>()

    val database = Database.connect("jdbc:sqlite:hopper.db", "org.sqlite.JDBC", manager = { ThreadLocalTransactionManager(it, Connection.TRANSACTION_SERIALIZABLE) })

    fun <T> transaction(statement: Transaction.() -> T): T {
        synchronized(HopperDatabase) {
            return transaction(TransactionManager.manager.defaultIsolationLevel, 3, statement)
        }
    }

    fun makeNewDatabase() {
        HopperDatabase.transaction {
            create(HopperRunner.Logins)
            create(HopperRunner.Sessions)
        }

        LOGGER.info("made new database")
    }

    fun addNewUser(userId: String, newUsername: String, encodedPassword: String) {
        val newUser = HopperDatabase.transaction {
            HopperRunner.Login.new {
                userid = userId
                username = newUsername
                password = encodedPassword
            }
        }

        LOGGER.info("made new user: ${newUser.username}")
    }

    fun getUser(username: String): HopperRunner.Login? {
        val user = HopperDatabase.transaction {
            HopperRunner.Login.find { HopperRunner.Logins.username eq username }.firstOrNull() ?: return@transaction null
        }

        LOGGER.info("tried to find user $username: ${user?.userid}")

        return user
    }

    fun getUserTokens(username: String): Set<String>? {
        val user = getUser(username)
        if (user == null) {
            LOGGER.info("failed to find tokens for $username")
            return null
        }

        val tokens = HopperDatabase.transaction {
            val sessions = HopperRunner.Session.find { HopperRunner.Sessions.userid eq user.userid }
            sessions.map { it.token }.toSet()
        }

        LOGGER.info("found ${tokens.size} tokens for $username")

        return tokens
    }

    fun addUserToken(userId: String, newToken: String) {
        HopperDatabase.transaction {
            HopperRunner.Session.new {
                userid = userId
                token = newToken
            }
        }

        LOGGER.info("added session token for $userId")
    }

}