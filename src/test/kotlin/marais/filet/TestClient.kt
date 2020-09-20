package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.pipeline.impl.DummyModule
import marais.filet.pipeline.impl.Pipeline
import marais.filet.transport.DummyTransport
import java.io.DataOutputStream
import kotlin.test.Test

object TestClient {

    @Test
    fun `nickel miguel`() = runBlocking {

        val client = Client(Pipeline(DummyModule)) {
            when (it.packetId.toInt()) {
                0 -> {

                }
            }
        }
        client.start(DummyTransport.Client, this)
        client.transmit {
            sendPacket(DummyPacket())
        }
    }

    class DummyPacket : Packet(0, 0) {
        override fun serializeData(output: DataOutputStream) {
            TODO("Not yet implemented")
        }

        override fun dataLength(): Int {
            TODO("Not yet implemented")
        }

    }
}
