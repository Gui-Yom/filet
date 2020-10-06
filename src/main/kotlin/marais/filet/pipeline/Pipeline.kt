package marais.filet.pipeline

import java.nio.ByteBuffer
import java.util.*

class Pipeline(modules: List<Module>) {

    constructor(vararg module: Module) : this(listOf(*module))

    /**
     * OUT direction, first module is the first to be executed when sending a packet
     */
    private val modules = LinkedList(modules)

    fun processIn(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer>? {
        var p: Pair<Any, ByteBuffer>? = obj to buf
        modules.asReversed().forEach {
            p = it.processIn(ctx, p!!.first, p!!.second)
            // Packet was blocked by module
            if (p == null)
                return null
        }
        return p
    }

    fun processOut(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer>? {
        var p: Pair<Any, ByteBuffer>? = obj to buf
        modules.forEach {
            p = it.processOut(ctx, p!!.first, p!!.second)
            // Packet was blocked by module
            if (p == null)
                return null
        }
        return p
    }

    /**
     * Add a module to be the last executed in OUTPUT direction. (or first in INPUT direction)
     */
    fun addLast(module: Module) {
        modules.addLast(module)
    }

    fun addFirst(module: Module) {
        modules.addFirst(module)
    }

    fun removeAll(predicate: (Module) -> Boolean) {
        modules.removeAll(predicate)
    }
}
