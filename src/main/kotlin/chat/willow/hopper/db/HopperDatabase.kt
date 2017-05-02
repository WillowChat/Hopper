package chat.willow.hopper.db

import chat.willow.hopper.loggerFor
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

object HopperDatabase {
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

    fun getUser(username: String): Login? {
        val user = transaction {
            Login.find { Logins.username eq username }.firstOrNull() ?: return@transaction null
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

        val tokens = transaction {
            val sessions = Session.find { Sessions.userid eq user.userid }
            sessions.map { it.token }.toSet()
        }

        LOGGER.info("found ${tokens.size} tokens for $username")

        return tokens
    }

    fun addUserToken(userId: String, newToken: String) {
        transaction {
            Session.new {
                userid = userId
                token = newToken
            }
        }

        LOGGER.info("added session token for $userId")
    }

}