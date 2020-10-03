package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.pipeline.impl.DummyModule
import marais.filet.transport.impl.DummyTransport
import java.nio.ByteBuffer
import kotlin.test.Test

object TestClient {

    @Test
    fun `test client dsl`() = runBlocking {

        val client = Client(this, DummyModule)
        client.handler {
            when (it) {
                is DummyPacket -> {
                    it.a
                }
            }
        }
        client.registerSerializer(DummyPacket)
        client.start(DummyTransport.Client)
        client.transmit {
            sendPacket(DummyPacket())
        }
    }

    class DummyPacket(val a: Int = 0) {

        companion object : PacketSerializer<DummyPacket>(0) {
            override fun read(buffer: ByteBuffer): DummyPacket = DummyPacket(buffer.int)

            override fun getPacketKClass() = DummyPacket::class

            override fun writeData(obj: DummyPacket, buffer: ByteBuffer): Int {
                buffer.putInt(obj.a)
                return 4
            }
        }
    }
}
