package marais.filet.pipeline

import java.nio.ByteBuffer
import java.util.*

class Pipeline(modules: List<Module>) {

    constructor(vararg module: Module) : this(listOf(*module))

    /**
     * IN direction
     */
    private val modules = LinkedList(modules)

    fun processIn(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer> {
        var p = obj to buf
        modules.forEach {
            p = it.processIn(ctx, p.first, p.second)
        }
        return p
    }

    fun processOut(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer> {
        var p = obj to buf
        modules.asReversed().forEach {
            p = it.processOut(ctx, p.first, p.second)
        }
        return p
    }

    fun addModule(module: Module) {
        modules.add(module)
    }
}
