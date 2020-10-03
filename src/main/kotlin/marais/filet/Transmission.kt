package marais.filet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import marais.filet.pipeline.Context
import marais.filet.pipeline.Pipeline
import marais.filet.utils.PriorityChannel
import java.nio.ByteBuffer

/**
 * An ephemeral stream bonding related packets together.
 */
interface Transmission {
    val transmitId: Int

    /**
     * Send packets through this transmission.
     */
    fun sendPacket(obj: Any, priority: Int = -1)
}

internal class DefaultTransmission internal constructor(
    val scope: CoroutineScope,
    override val transmitId: Int,
    val serializers: HashMap<Byte, PacketSerializer<Any>>,
    val pipeline: Pipeline,
    val queue: PriorityChannel<Pair<Int, ByteBuffer>>
) : Transmission {
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


