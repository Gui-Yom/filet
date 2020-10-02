package marais.filet

import marais.filet.pipeline.Module
import marais.filet.pipeline.Pipeline
import java.io.Closeable

abstract class BaseEndpoint(vararg modules: Module) : Closeable {

    protected val pipeline = Pipeline(*modules)
    protected val serializers = HashMap<Byte, PacketSerializer<Any>>()

    /**
     * Registers a packet serializer.
     */
    @SuppressWarnings("unchecked")
    fun registerSerializer(vararg serializers: PacketSerializer<*>) {
        for (ser in serializers)
            this.serializers[ser.packetId] = ser as PacketSerializer<Any>
    }
}
