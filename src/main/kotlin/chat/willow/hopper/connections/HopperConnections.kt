package chat.willow.hopper.connections

import chat.willow.hopper.auth.IIdentifierGenerator
import chat.willow.hopper.auth.nextUniqueId
import chat.willow.hopper.logging.loggerFor
import chat.willow.warren.IWarrenClient
import chat.willow.warren.WarrenClient
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

interface IHopperConnections {

    fun add(host: String, port: Int, tls: Boolean, nick: String): HopperConnection?
    fun remove(id: String)
    fun all(): Set<HopperConnection>
    fun start(id: String)
    fun stop(id: String)

    operator fun get(id: String): HopperConnection?
    operator fun minusAssign(id: String)
    operator fun contains(id: String): Boolean

}

data class HopperConnection(val id: String, val info: HopperConnectionInfo)

data class HopperConnectionInfo(val host: String, val port: Int, val tls: Boolean, val nick: String)

enum class HopperConnectionState { STARTED, STOPPED }

data class HopperBuffer(val id: String, val events: List<HopperBufferEvent<*>>) // todo: think about storage and retrieval by id, date/time

data class HopperBufferEvent<out T>(val id: String, val type: String, val payload: T) // todo: metadata

class HopperConnections(private val generator: IIdentifierGenerator) : IHopperConnections {

    private val LOGGER = loggerFor<HopperConnections>()

    // todo: concurrency
    private val infos = ConcurrentHashMap<String, HopperConnection>()
    private val warrens = ConcurrentHashMap<String, IWarrenClient>()
    private val states = ConcurrentHashMap<String, HopperConnectionState>()
    private val threads = ConcurrentHashMap<String, Thread>()

    override fun get(id: String): HopperConnection? {
        return infos[id]
    }

    override fun minusAssign(id: String) {
        val info = infos[id] ?: return

        stop(info.id)

        infos.remove(id)
        warrens.remove(id)
        states.remove(id)
        threads.remove(id)
    }

    override fun contains(id: String): Boolean {
        return infos.containsKey(id)
    }

    override fun all(): Set<HopperConnection> {
        return infos.values.toSet()
    }

    override fun add(host: String, port: Int, tls: Boolean, nick: String): HopperConnection? {
        // todo: check for duplicates

        val id = nextUniqueId(generator, infos)
        
        val info = HopperConnectionInfo(host, port, tls, nick)
        
        val connection = HopperConnection(id, info)
        infos += id to connection
        val client = constructWarrenClient(info)

        client.events.onAny {
            LOGGER.info("event for $id: $it")
        }

        warrens += id to client
        states += id to HopperConnectionState.STOPPED

        return connection
    }

    override fun remove(id: String) {
        infos.remove(id)
        warrens.remove(id)
        states.remove(id)
    }

    private fun constructWarrenClient(info: HopperConnectionInfo): IWarrenClient {
        return WarrenClient.build {
            server(server = info.host) {
                useTLS = info.tls
                port = info.port
                channel("#botdev")
            }

            user(info.nick)
        }
    }

    override fun start(id: String) {
        val state = states[id] ?: return
        val warren = warrens[id] ?: return

        if (state == HopperConnectionState.STARTED) {
            return
        }

        // todo: double check thread

        val thread = thread(start = false, name = "Warren thread for $id") {
            this.states[id] = HopperConnectionState.STARTED

            LOGGER.info("Warren starting for $id")
            warren.start()
            LOGGER.info("Warren ended for $id")

            this.states[id] = HopperConnectionState.STOPPED
        }

        threads[id] = thread

        thread.start()
    }

    override fun stop(id: String) {
        val state = states[id] ?: return
        val thread = threads[id] ?: return
        val info = infos[id] ?: return

        if (state == HopperConnectionState.STOPPED) {
            return
        }

        LOGGER.info("Interrupting Warren for $id")
        thread.interrupt()
        thread.join(1000)

        // todo: recreate warren info?

        LOGGER.info("Warren for $id ended after being interrupted")
    }
}