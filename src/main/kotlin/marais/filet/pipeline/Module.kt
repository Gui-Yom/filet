package marais.filet.pipeline

import marais.filet.PacketSerializer
import java.nio.ByteBuffer

interface Module {
    fun processIn(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer>

    fun processOut(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer>
}

data class Context(val serializer: PacketSerializer<Any>,
                   val serializers: Map<Byte, PacketSerializer<Any>>,
                   val transmission: Int,
                   val priority: Int?)
