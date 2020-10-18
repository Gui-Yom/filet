package marais.filet

import java.nio.ByteBuffer
import kotlin.reflect.KClass

/**
 * Base class for objects that convert objects to/from bytes.
 * It also exposes some useful metadata to the client (like packet id and priority).
 */
abstract class GlobalPacketSerializer(val registry: ObjectRegistry) {

    fun <T : Any> deserialize(buffer: ByteBuffer, packetId: PacketId): T {
        val clazz = registry[packetId] as? KClass<T> ?: throw SerializerUnavailable(packetId)
        return deserialize(buffer, clazz)
    }

    abstract fun <T : Any> deserialize(buffer: ByteBuffer, clazz: KClass<T>): T

    abstract fun <T : Any> serialize(obj: T, out: ByteBuffer)
}

typealias PacketId = Byte

const val MAX_PACKET_SIZE = 8192

class DefaultGlobalSerializer(vararg serializers: CustomPacketSerializer<*>) :
    GlobalPacketSerializer(ObjectRegistry(serializers.map { Triple(it.getPacketKClass(), it.packetId, it.priority) })) {

    val serializers = mutableMapOf<KClass<*>, CustomPacketSerializer<Any>>()

    init {
        serializers.forEach {
            addSerializer(it as CustomPacketSerializer<Any>)
        }
    }

    private fun <T : Any> getSer(clazz: KClass<T>): CustomPacketSerializer<T>? {
        return serializers[clazz] as? CustomPacketSerializer<T>
    }

    fun addSerializer(ser: CustomPacketSerializer<Any>) {
        if (this.serializers.values.find { it.packetId == ser.packetId } != null)
            throw IllegalArgumentException("A serializer for the same packetId has already been registered")
        if (this.serializers[ser.getPacketKClass()] != null)
            throw IllegalArgumentException("A serializer for the same class has already been registered")
        this.serializers[ser.getPacketKClass()] = ser
    }

    override fun <T : Any> deserialize(buffer: ByteBuffer, clazz: KClass<T>): T {
        val s = getSer(clazz) ?: throw SerializerUnavailable(clazz)
        return s.deserialize(buffer)
    }

    override fun <T : Any> serialize(obj: T, out: ByteBuffer) {
        val s = getSer(obj::class) ?: throw SerializerUnavailable(obj::class)
        s.serialize(obj, out)
    }
}

/**
 * Base class for a custom object serializer. Subclass this to provide a serializer for your class.
 */
abstract class CustomPacketSerializer<T : Any>(val packetId: PacketId, val priority: Int = 0) {

    /**
     * Override this method to write your own data to the packet.
     *
     * @return the number of bytes written
     */
    abstract fun serialize(obj: T, buffer: ByteBuffer): Int

    abstract fun deserialize(buffer: ByteBuffer): T

    abstract fun getPacketKClass(): KClass<T>
}
