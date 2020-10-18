package marais.filet.pipeline

import marais.filet.PacketId
import java.nio.ByteBuffer

interface ObjectModule {
    fun processIn(ctx: Context, obj: Any): Any?

    fun processOut(ctx: Context, obj: Any): Any?
}

interface BytesModule {
    fun processIn(ctx: Context, buf: ByteBuffer): ByteBuffer?

    fun processOut(ctx: Context, buf: ByteBuffer): ByteBuffer?
}

abstract class ObjectModuleAdapter : ObjectModule {
    override fun processIn(ctx: Context, obj: Any): Any? = obj

    override fun processOut(ctx: Context, obj: Any): Any? = obj
}

abstract class BytesModuleAdapter : BytesModule {
    override fun processIn(ctx: Context, buf: ByteBuffer): ByteBuffer? = buf

    override fun processOut(ctx: Context, buf: ByteBuffer): ByteBuffer? = buf
}

data class Context(
    val transmission: Int,
    val packetId: PacketId,
    val priority: Int?
)
