package chat.willow.hopper.model.rest

data class ServersGetResponseBody(val servers: Set<Server>)

data class Server(val id: String, val server: String, val nick: String)