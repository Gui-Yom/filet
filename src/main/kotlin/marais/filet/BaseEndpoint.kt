package marais.filet

import marais.filet.pipeline.Module
import marais.filet.pipeline.Pipeline
import java.io.Closeable

/**
 * Holds common tools like serializers and modules.
 */
abstract class BaseEndpoint internal constructor(
    internal val pipeline: Pipeline,
    internal val serializers: MutableMap<Byte, PacketSerializer<Any>> = mutableMapOf()
) : Closeable {

    internal constructor(vararg modules: Module) : this(Pipeline(*modules))

    /**
     * true if the endpoint is closed
     */
    protected var isClosed = false

    /**
     * Registers object serializers and deserializers.
     */
    @SuppressWarnings("unchecked")
    fun registerSerializer(vararg serializers: PacketSerializer<*>) {
        for (ser in serializers) {
            if (ser.packetId in this.serializers)
                throw IllegalArgumentException("A serializer for the same packetId has already been registered")
            this.serializers[ser.packetId] = ser as PacketSerializer<Any>
        }
    }

    override fun close() {
        isClosed = true
    }
}
