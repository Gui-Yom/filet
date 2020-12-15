package marais.filet.pipeline.impl

import marais.filet.contentToString
import marais.filet.pipeline.BytesModule
import marais.filet.pipeline.Context
import marais.filet.pipeline.ObjectModule
import java.nio.ByteBuffer

object SoutModule : BytesModule, ObjectModule {

    override fun processIn(ctx: Context, obj: Any): Any? {
        println("received : $obj")
        return obj
    }

    override fun processOut(ctx: Context, obj: Any): Any? {
        println("sent : $obj")
        return obj
    }

    override fun processIn(ctx: Context, buf: ByteBuffer): ByteBuffer? {
        println("received : ${buf.contentToString()}")
        return buf
    }

    override fun processOut(ctx: Context, buf: ByteBuffer): ByteBuffer? {
        println("sent : ${buf.contentToString()}")
        return buf
    }
}
