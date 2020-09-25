package marais.filet.pipeline.impl

import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import java.nio.ByteBuffer
import java.util.*

class Pipeline(modules: List<Module>) : Module {

    constructor(vararg module: Module) : this(listOf(*module))

    /**
     * IN direction
     */
    private val modules = LinkedList(modules)

    override fun processIn(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer> {
        var p = obj to buf
        modules.forEach {
            p = it.processIn(ctx, p.first, p.second)
        }
        return p
    }

    override fun processOut(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer> {
        var p = obj to buf
        modules.asReversed().forEach {
            p = it.processOut(ctx, p.first, p.second)
        }
        return p
    }
}
