package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.pipeline.impl.DummyModule
import marais.filet.pipeline.impl.EncryptionModule
import marais.filet.pipeline.impl.Pipeline
import marais.filet.transport.impl.DummyTransport
import java.nio.ByteBuffer
import kotlin.test.Test

object TestClient {

    @Test
    fun `test client dsl`() = runBlocking {

        val client = Client(Pipeline(DummyModule)) {
            when (it.packetId.toInt()) {
                0 -> {

                }
            }
        }
        client.register(EncryptionModule.EncryptedPacket to EncryptionModule.EncryptedPacket)
        client.start(DummyTransport.Client, this)
        client.transmit {
            sendPacket(DummyPacket())
        }
    }

    class DummyPacket(val a: Int = 0) : Packet(0, 0) {
        override fun serializeData(buffer: ByteBuffer): Int {
            buffer.putInt(a)
            return 4
        }

        companion object Reader : PacketReader<DummyPacket>({
            DummyPacket(readInt())
        })
    }
}
