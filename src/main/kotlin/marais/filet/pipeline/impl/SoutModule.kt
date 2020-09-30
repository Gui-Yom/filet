package marais.filet.pipeline.impl

import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import java.nio.ByteBuffer

object SoutModule : Module {
    override fun processIn(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer> {
        println("received : ${ctx.transmission}, ${ctx.priority}")
        return obj to buf
    }

    override fun processOut(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer> {
        return obj to buf
    }
}