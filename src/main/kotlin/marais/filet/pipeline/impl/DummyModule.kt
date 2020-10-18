package marais.filet.pipeline.impl

import marais.filet.pipeline.BytesModule
import marais.filet.pipeline.Context
import marais.filet.pipeline.ObjectModule
import java.nio.ByteBuffer

object DummyObjectModule : ObjectModule {
    override fun processIn(ctx: Context, obj: Any): Any? = obj

    override fun processOut(ctx: Context, obj: Any): Any? = obj
}

object DummyBytesModule : BytesModule {
    override fun processIn(ctx: Context, buf: ByteBuffer): ByteBuffer? = buf

    override fun processOut(ctx: Context, buf: ByteBuffer): ByteBuffer? = buf
}
