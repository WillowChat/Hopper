package chat.willow.hopper.db

import chat.willow.hopper.loggerFor
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

data class UserLogin(val userId: String, val user: String, val password: String)

interface ILoginDataSource {
    fun getUserLogin(user: String): UserLogin?
}

interface ITokenDataSink {
    fun addUserToken(userId: String, newToken: String): Boolean
}

interface ITokensDataSource {
    fun getUserTokens(username: String): Set<String>?
}

object HopperDatabase : ILoginDataSource, ITokenDataSink, ITokensDataSource {
    private val LOGGER = loggerFor<HopperDatabase>()

    val database = Database.connect("jdbc:sqlite:hopper.db", "org.sqlite.JDBC", manager = { ThreadLocalTransactionManager(it, Connection.TRANSACTION_SERIALIZABLE) })

    fun <T> transaction(statement: Transaction.() -> T): T {
        synchronized(HopperDatabase) {
            return org.jetbrains.exposed.sql.transactions.transaction(TransactionManager.manager.defaultIsolationLevel, 3, statement)
        }
    }

    fun makeNewDatabase() {
        transaction {
            SchemaUtils.create(Logins)
            SchemaUtils.create(Sessions)
        }

        LOGGER.info("made new database")
    }

    fun addNewUser(userId: String, newUsername: String, encodedPassword: String) {
        val newUser = transaction {
            Login.new {
                userid = userId
                username = newUsername
                password = encodedPassword
            }
        }

        LOGGER.info("made new user: ${newUser.username}")
    }

    override fun getUserLogin(user: String): UserLogin? {
        val dbUserLogin = transaction {
            Login.find { Logins.username eq user }.firstOrNull() ?: return@transaction null
        } ?: return null

        LOGGER.info("tried to find user $user: ${dbUserLogin.userid}")

        return UserLogin(userId = dbUserLogin.userid, user = dbUserLogin.username, password = dbUserLogin.password)
    }

    override fun getUserTokens(username: String): Set<String>? {
        val user = getUserLogin(username)
        if (user == null) {
            LOGGER.info("failed to find tokens for $username")
            return null
        }

        val tokens = transaction {
            val sessions = Session.find { Sessions.userid eq user.userId }
            sessions.map { it.token }.toSet()
        }

        LOGGER.info("found ${tokens.size} tokens for $username")

        return tokens
    }

    override fun addUserToken(userId: String, newToken: String): Boolean {
        transaction {
            Session.new {
                userid = userId
                token = newToken
            }
        }
        // todo: handle failure?

        LOGGER.info("added session token for $userId")

        return true
    }

}