package chat.willow.hopper.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

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
