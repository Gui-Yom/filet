package marais.filet

import marais.filet.pipeline.Module
import marais.filet.pipeline.Pipeline

abstract class BaseEndpoint(vararg modules: Module) {

    protected val pipeline = Pipeline(*modules)
    protected val serializers = HashMap<Byte, PacketSerializer<Any>>()

    /**
     * Registers a packet serializer.
     */
    @SuppressWarnings("unchecked")
    fun registerType(serializer: PacketSerializer<*>) {
        this.serializers[serializer.packetId] = serializer as PacketSerializer<Any>
    }
}
