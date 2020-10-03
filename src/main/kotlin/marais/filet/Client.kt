package marais.filet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import marais.filet.transport.ClientTransport
import marais.filet.utils.PriorityChannel
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

typealias ClientPacketHandler = suspend Client.(obj: Any) -> Unit

/**
 * Manage a connection to a server.
 */
class Client(private val scope: CoroutineScope, vararg modules: Module) : BaseEndpoint(*modules) {

    private val queue = PriorityChannel(Comparator.comparingInt(Pair<Int, ByteBuffer>::first).reversed())
    private val nextTransmissionId = AtomicInteger(0)
    private var transport: ClientTransport? = null
    private var packetHandler: ClientPacketHandler = { }

    /**
     * Infinite send loop, takes buffer from the queue and write them to the transport
     */
    private var sendJob: Job? = null

    /**
     * Infinite receive loop, reads packets from the transport and sends them down to the pipeline and the packetHandler
     */
    private var receiveJob: Job? = null

    /**
     * Set the receiver block, this block will be called each time a packet is received and can be called concurrently.
     *
     * @param handler the packet handler
     */
    fun handler(handler: ClientPacketHandler) {
        this.packetHandler = handler
    }

    /**
     * Initialize the underlying transport and start the receiver and sender loops.
     * It is possible to [transmit] after calling this method.
     */
    suspend fun start(transport: ClientTransport) {
        this.transport = transport

        // Init connection
        transport.init()

        // Receiver loop
        // We take a reference to the job in order to stop it when closing the Client
        receiveJob = scope.launch(context = Dispatchers.IO) {
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
                scope.launch(context = Dispatchers.Default) {
                    val (newPacket, buf) = pipeline.processIn(ctx, ctx.serializer.read(data), total)
                    packetHandler(this@Client, newPacket)
                }
            }
        }
        // Infinite sender loop on an IO thread
        sendJob = scope.launch(context = Dispatchers.IO) {
            while (true) {
                val (_, buf) = queue.receive()
                val position = buf.position()
                buf.reset()
                buf.limit(position)
                transport.writeBytes(buf)
            }
        }
    }

    /**
     * Opens up a new transmission to send packets to the underlying transport.
     */
    fun transmit(block: Transmission.() -> Unit) {
        block(Transmission(nextTransmissionId.getAndIncrement()))
    }

    inner class Transmission(val transmission: Int) {
        fun sendPacket(obj: Any, priority: Int? = -1) {
            // TODO use a backbuffer
            // TODO use OKIO
            // TODO use a buffer pool
            scope.launch {
                val buffer = ByteBuffer.allocate(PacketSerializer.MAX_PACKET_SIZE * 2)
                buffer.mark()
                val serializer = serializers.values.find { it.getPacketKClass() == obj::class }
                    ?: throw SerializerUnavailable(obj::class)
                serializer.write(transmission, obj, buffer)
                val effectivePriority = if (priority == null || priority == -1) serializer.priority else priority
                val ctx = Context(serializer, serializers, transmission, effectivePriority)
                queue.send(effectivePriority to pipeline.processOut(ctx, obj, buffer).second)
            }
        }
    }

    override fun close() {
        queue.close()
        sendJob?.cancel()
        receiveJob?.cancel()
        transport?.close()
    }
}
