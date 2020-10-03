package marais.filet.pipeline.impl

import marais.filet.PacketSerializer
import marais.filet.pipeline.Context
import marais.filet.pipeline.Module
import java.nio.ByteBuffer

class EncryptionModule : Module {

    override fun processIn(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer> {
        TODO("Not yet implemented")
    }

    override fun processOut(ctx: Context, obj: Any, buf: ByteBuffer): Pair<Any, ByteBuffer> {
        return EncryptedPacket(obj) to buf
    }

    class EncryptedPacket(obj: Any) : PacketSerializer<EncryptedPacket>(-1) {

        override fun read(buffer: ByteBuffer): EncryptedPacket {
            val size = buffer.int
            // TODO AES decryption
            return EncryptedPacket("")
        }

        override fun writeData(obj: EncryptedPacket, buffer: ByteBuffer): Int {
            // TODO AES encryption
            buffer.putInt(0)
            buffer.put(buffer.array(), 0, buffer.position())
            return 4
        }

        override fun getPacketKClass() = EncryptedPacket::class
    }
}
