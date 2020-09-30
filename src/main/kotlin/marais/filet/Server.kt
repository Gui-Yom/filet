package marais.filet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import marais.filet.transport.ClientTransport
import marais.filet.transport.ServerTransport
import marais.filet.utils.PriorityChannel
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator

/**
 * The server listen for connections from clients.
 */
class Server(vararg modules: Module) : BaseEndpoint(*modules) {

    private var transport: ServerTransport? = null
    private var handler: suspend Remote.(Any) -> Unit = { }

    val clients = Collections.synchronizedList(mutableListOf<Remote>())
        get() = field

    /**
     * Set the receiver block, this block will be called each time a packet is received and can be called concurrently.
     *
     * @param handler the packet handler
     */
    fun handler(handler: suspend Remote.(Any) -> Unit) {
        this.handler = handler
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
                    clients.add(remote)
                    // TODO callback-based event on new connection ?

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
                                val (newPacket, buf) = pipeline.processIn(ctx, ctx.serializer.read(dataBuf), total)
                                handler(remote, newPacket)
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
    inner class Remote(val transport: ClientTransport) {

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
                        val effectivePriority = if (priority == null || priority == -1) serializer.priority else priority
                        val ctx = Context(serializer, serializers, transmission, effectivePriority)
                        queue.send(effectivePriority to pipeline.processOut(ctx, obj, buffer).second)
                    }
                }
            }
        }
    }
}
