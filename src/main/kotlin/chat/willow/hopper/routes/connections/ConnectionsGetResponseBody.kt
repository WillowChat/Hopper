package chat.willow.hopper.routes.connections

data class ConnectionsGetResponseBody(val connections: Set<Server>)

data class Server(val id: String, val server: String, val nick: String)