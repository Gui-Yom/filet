package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.pipeline.impl.DummyModule
import marais.filet.transport.impl.DummyTransport
import java.nio.ByteBuffer
import kotlin.test.Test

object TestClient {

    @Test
    fun `test client dsl`() = runBlocking {

        val client = Client(DummyModule)
        client.handler {
            when (it) {
                is DummyPacket -> {
                    it.a
                }
            }
        }
        client.registerType(DummyPacket)
        client.start(DummyTransport.Client)
        client.transmit {
            sendPacket(DummyPacket())
        }
    }

    class DummyPacket(val a: Int = 0) {

        companion object : AbstractPacketSerializer<DummyPacket>(0) {
            override fun read(buffer: ByteBuffer): DummyPacket = DummyPacket(buffer.int)

            override fun getPacketClass(): Class<DummyPacket> = DummyPacket::class.java

            override fun writeData(obj: DummyPacket, buffer: ByteBuffer): Int {
                buffer.putInt(obj.a)
                return 4
            }
        }
    }
}
