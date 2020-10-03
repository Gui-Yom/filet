package marais.filet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import marais.filet.pipeline.Module
import marais.filet.transport.ServerTransport
import java.util.*

typealias ServerPacketHandler = suspend Client.(Server, obj: Any) -> Unit
typealias ConnectionHandler = suspend Client.(Server) -> Boolean

/**
 * The server listen for connections from clients.
 */
class Server(internal val scope: CoroutineScope, vararg modules: Module) : BaseEndpoint(*modules) {

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
                if (connectionHandler(remote, this@Server)) {
                    remote.start()
                    clients.add(remote)
                } else {
                    remote.close()
                }
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
