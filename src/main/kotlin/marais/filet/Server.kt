package marais.filet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import marais.filet.pipeline.Pipeline
import marais.filet.transport.ServerTransport
import java.util.*

typealias ServerPacketHandler = suspend Server.(it: Client, obj: Any) -> Unit
typealias ConnectionHandler = suspend Server.(it: Client) -> Boolean

/**
 * The server listen for connections from clients.
 */
class Server(internal val scope: CoroutineScope, pipeline: Pipeline) : BaseEndpoint(pipeline) {

    private var transport: ServerTransport? = null

    internal var packetHandler: ServerPacketHandler = { _, _ -> }
    private var connectionHandler: ConnectionHandler = { true }

    private var acceptJob: Job? = null

    val clients: MutableList<Client> = Collections.synchronizedList(mutableListOf<Client>())

    /**
     * Set the receiver block, this block will be called each time a packet is received and can be called concurrently.
     *
     * @param handler the packet handler
     */
    fun handler(handler: ServerPacketHandler) {
        this.packetHandler = handler
    }

    /**
     * Block to be called on each new connection,
     * plz don't suspend too long here as this is blocking the accept loop
     *
     * @param handler the connection handler, should return true to accept the new connection
     */
    fun connectionHandler(handler: ConnectionHandler) {
        this.connectionHandler = handler
    }

    suspend fun start(transport: ServerTransport) {

        this.transport = transport

        transport.init()
        acceptJob = scope.launch(Dispatchers.IO) {
            // Infinite accept loop
            while (true) {
                val remote = Client(transport.accept(), this@Server)

                // TODO do not block the accept loop
                if (connectionHandler(this@Server, remote)) {
                    remote.start()
                    clients.add(remote)
                } else {
                    remote.close()
                }
            }
        }
    }

    /**
     * Broadcast a packet to all of connected clients.
     */
    suspend fun broadcast(obj: Any) {
        clients.forEach {
            it.send {
                send(obj)
            }
        }
    }

    /**
     * Broadcast a packet to all of connected clients that matches the predicate.
     */
    suspend fun broadcast(obj: Any, predicate: (Client) -> Boolean) {
        clients.forEach {
            if (predicate(it))
                it.send {
                    send(obj)
                }
        }
    }

    /**
     * Shutdown the underlying transport and cancel any jobs or resources associated with this Server.
     */
    override fun close() {
        super.close()
        acceptJob?.cancel()
        clients.forEach {
            it.close()
        }
        transport?.close()
    }
}
