package marais.filet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import marais.filet.pipeline.Module
import marais.filet.transport.ClientTransport
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manage a connection to a server.
 */
class Client(val scope: CoroutineScope,
             private val module: Module,
             private val readers: Map<Packet, PacketReader<Packet>>,
             private val onReceive: (Packet) -> Unit) {

    private val queue = PriorityChannel(Comparator.comparingInt { it: Pair<Packet, ByteBuffer> -> it.first.priority }.reversed())
    private val nextTransmissionId = AtomicInteger(0)
    private var transport: ClientTransport? = null

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

                val reader = readers.get(header[4])
                require(reader != null)
                val (newPacket, buf) = module.processIn(reader.read(total), total)
                onReceive(newPacket)
            }
        }
        scope.launch {
            while (true) {
                val (packet, buf) = queue.receive()
                val position = buf.position()
                buf.reset()
                buf.limit(position)
                transport.writeBytes(buf)
            }
        }
    }

    fun transmit(block: Transmission.() -> Unit) {
        block(Transmission())
    }

    inner class Transmission {
        fun sendPacket(packet: Packet) {
            scope.launch {
                if (packet.transmission == null)
                    packet.transmission = nextTransmissionId.getAndIncrement()
                // TODO use a backbuffer
                // TODO use OKIO
                // TODO use a buffer pool
                val buffer = ByteBuffer.allocate(Packet.MAX_PACKET_SIZE * 2)
                packet.writeTo(buffer)
                queue.send(module.processOut(packet, buffer))
            }
        }
    }
}
