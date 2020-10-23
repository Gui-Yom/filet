package marais.filet.pipeline

import marais.filet.PacketSerializer
import java.nio.ByteBuffer
import java.util.*

/**
 * Process objects and bytes when sent or received.
 */
class Pipeline(
    objectModules: List<ObjectModule>,
    val serializer: PacketSerializer,
    bytesModules: List<BytesModule>
) {

    constructor(serializer: PacketSerializer) : this(listOf(), serializer, listOf())

    /**
     * OUT direction, first module is the first to be executed when sending a packet
     */
    private val objectModules = LinkedList(objectModules)

    /**
     * OUT direction
     */
    private val bytesModules = LinkedList(bytesModules)

    /**
     * IN direction
     */
    private val objectModulesRev = objectModules.asReversed()

    /**
     * IN direction
     */
    private val bytesModulesRev = bytesModules.asReversed()

    fun processIn(ctx: Context, buf: ByteBuffer): Any? {
        var b: ByteBuffer? = buf
        bytesModulesRev.forEach {
            b!!.rewind()
            b = it.processOut(ctx, b!!)

            if (b == null)
                return null
        }

        var p: Any? = serializer.deserialize(buf, ctx.packetId)
        objectModulesRev.forEach {
            p = it.processOut(ctx, p!!)
            // Packet was blocked by module
            if (p == null)
                return null
        }
        return p
    }

    fun processOut(ctx: Context, obj: Any, buf: ByteBuffer): ByteBuffer? {
        var p: Any? = obj
        objectModules.forEach {
            p = it.processOut(ctx, p!!)
            // Packet was blocked by module
            if (p == null)
                return null
        }

        serializer.serialize(obj, buf)

        var b: ByteBuffer? = buf
        bytesModules.forEach {
            b!!.rewind()
            b = it.processOut(ctx, b!!)

            if (b == null)
                return null
        }
        return buf
    }

    /**
     * Add a module to be the last executed in OUTPUT direction. (or first in INPUT direction)
     */
    fun addLast(module: ObjectModule) {
        objectModules.addLast(module)
    }

    /**
     * Add a module to be the first executed in OUTPUT direction. (or last in INPUT direction)
     */
    fun addFirst(module: ObjectModule) {
        objectModules.addFirst(module)
    }

    /**
     * Add a module to be the last executed in OUTPUT direction. (or first in INPUT direction)
     */
    fun addLast(module: BytesModule) {
        bytesModules.addLast(module)
    }

    /**
     * Add a module to be the first executed in OUTPUT direction. (or last in INPUT direction)
     */
    fun addFirst(module: BytesModule) {
        bytesModules.addFirst(module)
    }

    fun removeAllO(predicate: (ObjectModule) -> Boolean) {
        objectModules.removeAll(predicate)
    }

    fun removeAllB(predicate: (BytesModule) -> Boolean) {
        bytesModules.removeAll(predicate)
    }

    fun removeO(module: ObjectModule) {
        objectModules.remove(module)
    }

    fun removeB(module: BytesModule) {
        bytesModules.remove(module)
    }
}
