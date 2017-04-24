package chat.willow.hopper.routes.servers

data class ServersGetResponseBody(val servers: Set<Server>)

data class Server(val id: String, val server: String, val nick: String)