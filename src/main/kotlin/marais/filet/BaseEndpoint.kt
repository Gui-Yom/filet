package marais.filet

import marais.filet.pipeline.Module
import marais.filet.pipeline.Pipeline
import java.io.Closeable

/**
 * Holds common tools like serializers and modules.
 */
abstract class BaseEndpoint internal constructor(protected val pipeline: Pipeline) : Closeable {

    internal constructor(vararg modules: Module) : this(Pipeline(*modules))

    protected val serializers = HashMap<Byte, PacketSerializer<Any>>()

    /**
     * true if the endpoint is closed
     */
    protected var isClosed = false

    /**
     * Registers object serializers and deserializers.
     */
    @SuppressWarnings("unchecked")
    fun registerSerializer(vararg serializers: PacketSerializer<*>) {
        for (ser in serializers)
            this.serializers[ser.packetId] = ser as PacketSerializer<Any>
    }

    override fun close() {
        isClosed = true
    }
}
