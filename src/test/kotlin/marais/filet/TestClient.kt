package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.pipeline.Pipeline
import marais.filet.pipeline.impl.DummyBytesModule
import marais.filet.pipeline.impl.DummyObjectModule
import marais.filet.transport.impl.DummyTransport
import java.nio.ByteBuffer
import kotlin.test.Test

object TestClient {

    @Test
    fun `test client dsl`() = runBlocking {

        val client = Client(
            this,
            Pipeline(listOf(DummyObjectModule), DefaultPacketSerializer(DummyPacket), listOf(DummyBytesModule))
        )
        client.handler {
            when (it) {
                is DummyPacket -> {
                    it.a
                }
            }
        }
        client.start(DummyTransport.Client)
        client.send {
            send(DummyPacket())
        }
    }

    class DummyPacket(val a: Int = 0) {
        companion object : CustomObjectSerializer<DummyPacket>(0) {
            override fun deserialize(buffer: ByteBuffer): DummyPacket = DummyPacket(buffer.int)

            override fun getPacketKClass() = DummyPacket::class

            override fun serialize(obj: DummyPacket, buffer: ByteBuffer): Int {
                buffer.putInt(obj.a)
                return 4
            }
        }
    }
}
