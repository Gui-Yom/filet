package marais.filet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import marais.filet.transport.ClientTransport
import marais.filet.utils.PriorityChannel
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

typealias ClientPacketHandler = suspend Client.(Any) -> Unit

/**
 * Manage a connection to a server.
 */
class Client(vararg modules: Module) : BaseEndpoint(*modules) {

    private val queue = PriorityChannel(Comparator.comparingInt(Pair<Int, ByteBuffer>::first).reversed())
    private val nextTransmissionId = AtomicInteger(0)
    private var transport: ClientTransport? = null
    private var handler: ClientPacketHandler = { }

    /**
     * Set the receiver block, this block will be called each time a packet is received and can be called concurrently.
     *
     * @param handler the packet handler
     */
    fun handler(handler: ClientPacketHandler) {
        this.handler = handler
    }

    /**
     * Start the connection
     */
    suspend fun start(transport: ClientTransport) {
        this.transport = transport

        // Init connection
        transport.init()

        // Receiver loop
        coroutineScope {
            launch(Dispatchers.IO) {
                // Infinite loop on an IO thread
                while (true) {
                    // TODO okio, fix terrible buffer allocation and copies
                    val header = ByteBuffer.allocate(4 + 1 + 4)
                    transport.readBytes(header)
                    val size = header.getInt(5)
                    header.reset()
                    val data = ByteBuffer.allocate(size)
                    transport.readBytes(data)
                    val total = ByteBuffer.allocate(4 + 1 + 4 + size).put(header).put(data)

                    val serializer = serializers[header[4]]
                    require(serializer != null)
                    val ctx = Context(serializer, serializers, header.getInt(0), null)

                    // Launch into Default thread pool to process modules and execute user code
                    launch(context = Dispatchers.Default) {
                        val (newPacket, buf) = pipeline.processIn(ctx, ctx.serializer.read(data), total)
                        handler(this@Client, newPacket)
                    }
                }
            }
            // Infinite sender loop on an IO thread
            launch(context = Dispatchers.IO) {
                while (true) {
                    val (packet, buf) = queue.receive()
                    val position = buf.position()
                    buf.reset()
                    buf.limit(position)
                    transport.writeBytes(buf)
                }
            }
        }
    }

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
                    val buffer = ByteBuffer.allocate(Companion.MAX_PACKET_SIZE * 2)
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

    companion object {
        const val MAX_PACKET_SIZE = 8192
    }

    override fun close() {
        queue.close()
        transport?.close()
    }
}
