package marais.filet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import marais.filet.transport.ClientTransport
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manage a connection to a server.
 */
class Client(val scope: CoroutineScope, private val module: Module) {

    private val queue = PriorityChannel(Comparator.comparingInt { it: Pair<Int, ByteBuffer> -> it.first }.reversed())
    private val nextTransmissionId = AtomicInteger(0)
    private var transport: ClientTransport? = null
    private var onReceive: (Client.(Any) -> Unit)? = null
    private val serializers = HashMap<Byte, PacketSerializer<Any>>()
    val MAX_PACKET_SIZE = 8192

    fun onReceive(onReceive: Client.(Any) -> Unit) {
        this.onReceive = onReceive
    }

    @SuppressWarnings("unchecked")
    fun registerType(serializer: PacketSerializer<*>) {
        this.serializers[serializer.packetId] = serializer as PacketSerializer<Any>
    }

    fun start(transport: ClientTransport) {
        this.transport = transport
        scope.launch {
            while (true) {
                // TODO okio, terrible buffer allocation and copies
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
                val (newPacket, buf) = module.processIn(ctx, serializer.read(data), total)
                onReceive!!(this@Client, newPacket)
            }
        }
        scope.launch {
            withContext(Dispatchers.IO) {
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

    fun transmit(block: Transmission.() -> Unit) {
        block(Transmission())
    }

    inner class Transmission {
        fun sendPacket(obj: Any, priority: Int? = -1) {
            scope.launch {
                // TODO use a backbuffer
                // TODO use OKIO
                // TODO use a buffer pool

                val buffer = ByteBuffer.allocate(MAX_PACKET_SIZE * 2)
                val serializer = serializers.values.find { it.getPacketClass() == obj::class.java }
                // TODO handle gracefully
                require(serializer != null)
                val transmission = nextTransmissionId.getAndIncrement()
                serializer.write(transmission, obj, buffer)
                val effectivePriority = if (priority == null || priority == -1) serializer.priority else priority
                val ctx = Context(serializer, serializers, transmission, effectivePriority)
                queue.send(effectivePriority to module.processOut(ctx, obj, buffer).second)
            }
        }
    }
}
