package marais.filet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import marais.filet.pipeline.BytesModule
import marais.filet.pipeline.Context
import marais.filet.pipeline.ObjectModule
import marais.filet.pipeline.Pipeline
import marais.filet.transport.ClientTransport
import marais.filet.utils.PriorityChannel
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

typealias ClientPacketHandler = suspend Client.(obj: Any) -> Unit

/**
 * Manage a connection to a server.
 */
class Client(
    private val scope: CoroutineScope,
    pipeline: Pipeline
) : BaseEndpoint(pipeline) {

    /**
     * @param scope the scope used to launch new coroutines
     */
    constructor(
        scope: CoroutineScope, objectModules: List<ObjectModule>,
        serializer: PacketSerializer,
        bytesModules: List<BytesModule>
    ) : this(scope, Pipeline(objectModules, serializer, bytesModules))

    /**
     * Used when creating the client from a remote connection in Server.
     */
    internal constructor(
        transport: ClientTransport,
        server: Server
    ) : this(server.scope, server.pipeline) {
        this.transport = transport
        this.server = server
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

    val isLocal = server == null

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
     * It is possible to [send] after calling this method.
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
            val header = ByteBuffer.allocate(4 + 1 + 4).mark()
            while (true) {
                header.reset()
                // TODO fix terrible buffer allocation and copies
                transport!!.readBytes(header)
                header.reset()
                val size = header.getInt(5)
                val data = ByteBuffer.allocate(size).mark()
                //println(header.array().contentToString())
                if (transport!!.readBytes(data) != size) {
                    // TODO handle gracefully
                    throw IllegalStateException("Finished early")
                }
                data.reset()

                // Launch into Default thread pool to process modules and execute user code
                launch(context = Dispatchers.Default) {
                    val obj = pipeline.processIn(Context(header.getInt(0), header.get(4), null), data)
                    if (obj != null) {
                        // Use the server packet handler when this client is a remote
                        if (server == null)
                            packetHandler(this@Client, obj)
                        else
                            server!!.packetHandler(server!!, this@Client, obj)
                    }
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
    suspend fun send(block: suspend Transmission.() -> Unit) {
        send(nextTransmissionId.getAndIncrement(), block)
    }

    /**
     * Opens up a new transmission to send packets to the underlying transport.
     *
     * @param transmitId the transmission id to use
     */
    suspend fun send(transmitId: Int, block: suspend Transmission.() -> Unit) {

        // TODO handle gracefully
        require(!isClosed)
        // TODO Maybe we could reuse the object, since only the transmission id is changed
        block(Transmission(transmitId))
    }

    suspend fun send(obj: Any) {
        send {
            send(obj)
        }
    }

    /**
     * Suspends till a packet of desired type arrives
     */
    suspend fun <T : Any> receive(clazz: KClass<T>): T = suspendCoroutine { cont ->
        pipeline.addFirst(object : ObjectModule {
            override fun processIn(ctx: Context, obj: Any): Any? {
                return if (obj::class == clazz) {
                    // Resume the coroutine
                    cont.resume(obj as T)
                    // Remove the module
                    // FIXME might be flawed
                    pipeline.removeO(this)
                    null
                } else obj
            }
        })
    }

    /**
     * Suspends till any packet arrives.
     */
    suspend fun receive(): Any = suspendCoroutine { cont ->
        pipeline.addFirst(object : ObjectModule {
            override fun processIn(ctx: Context, obj: Any): Any? {
                cont.resume(obj)
                pipeline.removeO(this)
                return null
            }
        })
    }

    /**
     * Shutdown the underlying transport and cancel any jobs or resources associated with this Client.
     */
    override fun close() {
        super.close()
        // Shutdown output
        queue.close()
        sendJob?.cancel()
        // Shutdown input
        receiveJob?.cancel()
        // Shutdown transport
        transport?.close()
    }

    inner class Transmission internal constructor(val transmitId: Int) {

        suspend fun send(obj: Any, priority: Int = -1) {
            // TODO use a backbuffer / buffer pool

            val packetId = registry[obj::class] ?: throw ClassUnregisteredException(obj::class)
            val effectivePriority = if (priority < 0)
            // if previous line doesn't throw we should be fine (modulo thread safety)
                registry.getPriority(obj::class)!!
            else priority

            // The pipeline context
            val ctx = Context(transmitId, packetId, effectivePriority)

            val data = pipeline.processOut(ctx, obj, ByteBuffer.allocate(MAX_PACKET_SIZE))
            if (data != null) {
                // Allocate space for the serialization
                // Here we should have a read buffer and a write buffer to send down the pipeline
                val buffer = ByteBuffer.allocate(9 + data.limit())
                buffer.putInt(transmitId) // transmission id
                buffer.put(packetId) // packet id
                buffer.putInt(data.limit()) // length
                buffer.put(data)
                // Sends the buffer to the sender loop, the queue will automatically sort buffers based on the priority
                queue.send(effectivePriority to buffer)
            }
        }
    }
}
