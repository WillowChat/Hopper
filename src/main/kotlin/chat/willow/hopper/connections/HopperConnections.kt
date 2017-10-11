package chat.willow.hopper.connections

import chat.willow.hopper.auth.IIdentifierGenerator
import chat.willow.hopper.auth.nextUniqueId
import chat.willow.hopper.logging.loggerFor
import chat.willow.warren.IWarrenClient
import chat.willow.warren.WarrenClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

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

    private val connectionLocks = ConcurrentHashMap<String, ReentrantLock>()
    private val connections = ConcurrentHashMap<String, HopperConnection>()

    private val warrens = mutableMapOf<String, IWarrenClient>()
    private val states = mutableMapOf<String, HopperConnectionState>()
    private val threads = mutableMapOf<String, Thread>()

    override fun get(id: String): HopperConnection? {
        return connectionLocks[id]?.withLock { connections[id] }
    }

    override fun minusAssign(id: String) {
        val connection = connections[id] ?: return
        val lock = connectionLocks[id] ?: return

        lock.withLock {
            stop(connection.id)

            connections.remove(id)
            warrens.remove(id)
            states.remove(id)
            threads.remove(id)
        }
    }

    override fun contains(id: String): Boolean {
        return connections.containsKey(id)
    }

    override fun all(): Set<HopperConnection> {
        return connections.values.toSet()
    }

    override fun add(host: String, port: Int, tls: Boolean, nick: String): HopperConnection? {
        // todo: check for duplicates?

        val id = nextUniqueId(generator, connections)
        val lock = ReentrantLock()

        connectionLocks[id] = lock

        return lock.withLock {
            val info = HopperConnectionInfo(host, port, tls, nick)

            val connection = HopperConnection(id, info)
            connections += id to connection
            val client = constructWarrenClient(info)

            client.events.onAny {
                LOGGER.info("event for $id: $it")
            }

            warrens += id to client
            states += id to HopperConnectionState.STOPPED

            connection
        }
    }

    override fun remove(id: String) {
        // todo: clean up bad state if found
        val lock = connectionLocks[id] ?: return

        lock.withLock {
            connections.remove(id)
            warrens.remove(id)
            states.remove(id)
        }
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
        val lock = connectionLocks[id] ?: return

        val thread = lock.withLock {
            val state = states[id] ?: return
            val warren = warrens[id] ?: return

            // todo: handle bad state
            if (state == HopperConnectionState.STARTED) {
                return
            }

            val thread = thread(start = false, name = "Warren thread for $id") {
                this.states[id] = HopperConnectionState.STARTED

                LOGGER.info("Warren starting for $id")
                warren.start()
                LOGGER.info("Warren ended for $id")

                stop(id)
            }

            thread.setUncaughtExceptionHandler { _, exception ->
                LOGGER.warn("uncaught exception for Warren $id: $exception")
                stop(id)
            }

            threads[id] = thread

            thread
        }

        thread.start()
    }

    override fun stop(id: String) {
        val lock = connectionLocks[id] ?: return

        lock.withLock {
            val state = states[id] ?: return
            val thread = threads[id] ?: return
            val connection = connections[id] ?: return

            if (state == HopperConnectionState.STOPPED) {
                return
            }

            LOGGER.info("stopping Warren by interrupting: $id")
            thread.interrupt()
            thread.join(1000)

            warrens[id] = constructWarrenClient(connection.info)
            states[id] = HopperConnectionState.STOPPED
        }
    }
}