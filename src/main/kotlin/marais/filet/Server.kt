package marais.filet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import marais.filet.transport.ClientTransport
import marais.filet.transport.ServerTransport
import marais.filet.utils.PriorityChannel
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator

typealias ServerPacketHandler = suspend Server.Remote.(Server, Any) -> Unit
typealias ConnectionHandler = suspend Server.Remote.(Server) -> Boolean

/**
 * The server listen for connections from clients.
 */
class Server(vararg modules: Module) : BaseEndpoint(*modules) {

    private var transport: ServerTransport? = null
    private var packetHandler: ServerPacketHandler = { _, _ -> }
    private var connectionHandler: ConnectionHandler = { true }

    val clients: MutableList<Remote> = Collections.synchronizedList(mutableListOf<Remote>())

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
        coroutineScope {
            launch {
                // Infinite accept loop
                while (true) {
                    val client = transport.accept()
                    val remote = Remote(client)
                    // TODO do not block the accept loop
                    if (connectionHandler(remote, this@Server))
                        clients.add(remote)
                    else {
                        client.close()
                        continue
                    }

                    launch(Dispatchers.IO) {
                        val headerBuf = ByteBuffer.allocate(4 + 1 + 4)
                        var dataBuf: ByteBuffer? = null
                        while (true) {
                            remote.transport.readBytes(headerBuf)
                            val size = headerBuf.getInt(5)
                            dataBuf = ByteBuffer.allocate(size)
                            remote.transport.readBytes(dataBuf)
                            val total = ByteBuffer.allocate(4 + 1 + 4 + size).put(headerBuf).put(dataBuf)

                            val serializer = serializers[headerBuf[4]]
                            require(serializer != null)
                            val ctx = Context(serializer, serializers, headerBuf.getInt(0), null)

                            // Launch into Default thread pool to process modules and execute user code
                            launch(context = Dispatchers.Default) {
                                val (newPacket, buf) = pipeline.processIn(
                                    ctx,
                                    // The buffer we pass here should be of the size of the data
                                    ctx.serializer.read(dataBuf),
                                    total
                                )
                                packetHandler(remote, this@Server, newPacket)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * A remote client connected to a local server
     */
    inner class Remote(val transport: ClientTransport) : Closeable {

        private val nextTransmissionId = AtomicInteger(0)
        private val queue = PriorityChannel(Comparator.comparingInt(Pair<Int, ByteBuffer>::first).reversed())

        suspend fun transmit(block: suspend Transmission.() -> Unit) {
            block(Transmission(nextTransmissionId.getAndIncrement()))
        }

        inner class Transmission(val transmission: Int) {
            suspend fun sendPacket(obj: Any, priority: Int? = -1) {
                // TODO use a backbuffer
                // TODO use OKIO
                // TODO use a buffer pool

                coroutineScope {
                    launch {
                        val buffer = ByteBuffer.allocate(Client.MAX_PACKET_SIZE * 2)
                        val serializer = serializers.values.find { it.getPacketClass() == obj::class.java }
                        // TODO handle gracefully
                        require(serializer != null)
                        serializer.write(transmission, obj, buffer)
                        val effectivePriority =
                            if (priority == null || priority == -1) serializer.priority else priority
                        val ctx = Context(serializer, serializers, transmission, effectivePriority)
                        queue.send(effectivePriority to pipeline.processOut(ctx, obj, buffer).second)
                    }
                }
            }
        }

        override fun close() {
            queue.close()
            transport.close()
        }
    }

    override fun close() {
        clients.forEach {
            it.close()
        }
        transport?.close()
    }
}
