package chat.willow.hopper.db

import chat.willow.hopper.generated.tables.Logins
import chat.willow.hopper.generated.tables.Sessions
import chat.willow.hopper.generated.tables.records.LoginsRecord
import chat.willow.hopper.logging.loggerFor
import org.flywaydb.core.Flyway
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import org.sqlite.SQLiteDataSource
import java.sql.DriverManager

data class UserLogin(val userId: String, val user: String, val encodedAuthEntry: String)

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

    private val DATABASE_URL = "jdbc:sqlite:hopper.db"
    val connection = DriverManager.getConnection(DATABASE_URL)

    fun doMigrations() {
        LOGGER.info("Running Flyway for database migrations...")

        val flyway = Flyway()
        val dataSource = SQLiteDataSource()
        dataSource.url = DATABASE_URL

        flyway.dataSource = dataSource
        flyway.migrate()
    }

    fun computeNumberOfUsers(): Int? {
        val context = DSL.using(connection)

        return try {
            context.fetchCount(Logins.LOGINS)
        } catch (exception: DataAccessException) {
            null
        }
    }

    fun addNewUser(userId: String, newUsername: String, encodedPassword: String): Boolean {
        val context = DSL.using(connection)

        val newLoginRecord = LoginsRecord()
        newLoginRecord[Logins.LOGINS.USERID] = userId
        newLoginRecord[Logins.LOGINS.USERNAME] = newUsername
        newLoginRecord[Logins.LOGINS.PASSWORD] = encodedPassword

        return try {
            context.insertInto(Logins.LOGINS).set(newLoginRecord).execute()

            LOGGER.info("made new user: $newUsername")
            true
        } catch (exception: DataAccessException) {
            LOGGER.info("failed to make new user: $newUsername")
            false
        }
    }

    override fun getUserLogin(user: String): UserLogin? {
        val context = DSL.using(connection)

        val query = context.selectFrom(Logins.LOGINS).where(Logins.LOGINS.USERNAME.equal(user))

        return try {
            val result = query.fetchOne()

            // todo: sanity check

            LOGGER.info("tried to find user login: $user")

            return UserLogin(userId = result.userid, user = result.username, encodedAuthEntry = result.password)
        } catch (exception: DataAccessException) {
            LOGGER.info("failed to get user login: $user")

            null
        }
    }

    override fun getUserTokens(username: String): Set<String>? {
        val user = getUserLogin(username)
        if (user == null) {
            LOGGER.info("failed to find user details for tokens: $username")
            return null
        }

        val context = DSL.using(connection)

        val query = context.selectFrom(Sessions.SESSIONS).where(Sessions.SESSIONS.USERID.equal(user.userId))

        return try {
            val results = query.fetch()

            results.mapNotNull { it.token }.toSet()
        } catch (exception: DataAccessException) {
            LOGGER.info("failed to query tokens: $username")

            null
        }
    }

    override fun addUserToken(userId: String, newToken: String): Boolean {
        val context = DSL.using(connection)

        val newUserTokenRecord = Sessions.SESSIONS.newRecord()
        newUserTokenRecord[Sessions.SESSIONS.USERID] = userId
        newUserTokenRecord[Sessions.SESSIONS.TOKEN] = newToken

        return try {
            context.insertInto(Sessions.SESSIONS).set(newUserTokenRecord).execute()

            LOGGER.info("added session token: $userId")

            true
        } catch (exception: DataAccessException) {
            LOGGER.info("failed to add session token: $userId")

            false
        }
    }

}