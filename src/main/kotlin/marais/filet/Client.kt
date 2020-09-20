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
class Client(val scope: CoroutineScope, private val module: Module, private val onReceive: (Packet) -> Unit) {

    private val queue = PriorityChannel(Comparator.comparingInt(Packet::priority).reversed())
    private val nextTransmissionId = AtomicInteger(0)
    private var transport: ClientTransport? = null
    private val buffer: ByteBuffer = ByteBuffer.allocate(8192)

    fun start(transport: ClientTransport) {
        this.transport = transport
        scope.launch {
            while (true) {
                val packet = transport.readBytes()
                module.processIn(packet)
                onReceive(packet)
            }
        }
        scope.launch {
            while (true) {
                val packet = queue.receive()
                packet.writeTo(ByteBufferOutputStream(buffer))
                transport.writeBytes(buffer)
                buffer.reset()
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
                queue.send(module.processOut(packet))
            }
        }
    }
}
