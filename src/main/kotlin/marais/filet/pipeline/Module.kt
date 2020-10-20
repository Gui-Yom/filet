package marais.filet.pipeline

import marais.filet.PacketId
import java.nio.ByteBuffer

interface ObjectModule {
    fun processIn(ctx: Context, obj: Any): Any? = obj

    fun processOut(ctx: Context, obj: Any): Any? = obj
}

interface BytesModule {
    fun processIn(ctx: Context, buf: ByteBuffer): ByteBuffer? = buf

    fun processOut(ctx: Context, buf: ByteBuffer): ByteBuffer? = buf
}

data class Context(
    val transmission: Int,
    val packetId: PacketId,
    val priority: Int?
)
