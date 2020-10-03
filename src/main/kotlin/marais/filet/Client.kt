package marais.filet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import marais.filet.pipeline.Pipeline
import marais.filet.transport.ClientTransport
import marais.filet.utils.PriorityChannel
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

typealias ClientPacketHandler = suspend Client.(obj: Any) -> Unit

/**
 * Manage a connection to a server.
 */
class Client(private val scope: CoroutineScope, pipeline: Pipeline) : BaseEndpoint(pipeline) {

    /**
     * @param scope the scope used to launch new coroutines
     */
    constructor(scope: CoroutineScope, vararg modules: Module) : this(scope, Pipeline(*modules))

    /**
     * Used when creating the client from a remote connection in Server.
     */
    internal constructor(scope: CoroutineScope, pipeline: Pipeline, transport: ClientTransport)
            : this(scope, pipeline) {
        this.transport = transport
    }

    private val queue = PriorityChannel(Comparator.comparingInt(Pair<Int, ByteBuffer>::first).reversed())

    /**
     * Used to generate the transmission id
     */
    private val nextTransmissionId = AtomicInteger(0)

    /**
     * The underlying transport
     */
    private var transport: ClientTransport? = null

    /**
     * Block called each time a packet is received
     */
    private var packetHandler: ClientPacketHandler = {}

    internal var server: Server? = null

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
        start()
    }

    internal suspend fun start() {
        // Init connection
        transport!!.init()

        // Receiver loop
        // We take a reference to the job in order to stop it when closing the Client
        receiveJob = scope.launch(context = Dispatchers.IO) {
            // Infinite loop on an IO thread
            while (true) {
                // TODO okio, fix terrible buffer allocation and copies
                val header = ByteBuffer.allocate(4 + 1 + 4)
                transport!!.readBytes(header)
                val size = header.getInt(5)
                header.reset()
                val data = ByteBuffer.allocate(size)
                transport!!.readBytes(data)
                val total = ByteBuffer.allocate(4 + 1 + 4 + size).put(header).put(data)

                val serializer = serializers[header[4]]
                require(serializer != null)
                val ctx = Context(serializer, serializers, header.getInt(0), null)

                // Launch into Default thread pool to process modules and execute user code
                scope.launch(context = Dispatchers.Default) {
                    val (obj, buf) = pipeline.processIn(ctx, ctx.serializer.read(data), total)
                    // Use the server packet handler when this client is a remote
                    if (server == null)
                        packetHandler(this@Client, obj)
                    else
                        server!!.packetHandler(this@Client, server!!, obj)
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
                transport!!.writeBytes(buf)
            }
        }
    }

    /**
     * Opens up a new transmission to send packets to the underlying transport.
     * The transmission id is automatically generated.
     */
    fun transmit(block: Transmission.() -> Unit) {
        transmit(nextTransmissionId.getAndIncrement(), block)
    }

    /**
     * Opens up a new transmission to send packets to the underlying transport.
     *
     * @param transmitId the transmission id to use
     */
    fun transmit(transmitId: Int, block: Transmission.() -> Unit) {

        // TODO handle gracefully
        require(!isClosed)
        // TODO Maybe we could reuse the object, since only the transmission id is changed
        block(DefaultTransmission(transmitId))
    }

    /**
     * Shutdown the underlying transport and cancel any jobs or resources associated with this Client.
     */
    override fun close() {
        super.close()
        // Shutdown input
        receiveJob?.cancel()
        // Shutdown output
        queue.close()
        sendJob?.cancel()
        transport?.close()
    }

    internal inner class DefaultTransmission(override val transmitId: Int) : Transmission {
        override fun sendPacket(obj: Any, priority: Int) {
            // TODO use a backbuffer
            // TODO use OKIO
            // TODO use a buffer pool
            // Return immediately after scheduling this coroutine.
            scope.launch {
                // Try to find an appropriate serializer for the object
                val serializer = serializers.values.find { it.getPacketKClass() == obj::class }
                    ?: throw SerializerUnavailable(obj::class)
                // Allocate space for the serialization
                // Here we should have a read buffer and a write buffer to send down the pipeline
                val buffer = ByteBuffer.allocate(PacketSerializer.MAX_PACKET_SIZE * 2)
                buffer.mark()
                // Serialization happens here
                serializer.write(transmitId, obj, buffer)

                val effectivePriority = if (priority < 0) serializer.priority else priority
                // The pipeline context
                val ctx = Context(serializer, serializers, transmitId, effectivePriority)

                // The final buffer we'll send to the transport
                val finalBuffer = pipeline.processOut(ctx, obj, buffer).second

                // Sends the buffer to the sender loop, the queue will automatically sort buffers based on the priority
                queue.send(effectivePriority to finalBuffer)
            }
        }
    }
}
