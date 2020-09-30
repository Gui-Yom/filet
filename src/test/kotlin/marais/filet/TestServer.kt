package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.pipeline.impl.DummyModule
import marais.filet.transport.impl.DummyTransport
import org.junit.jupiter.api.Test
import java.nio.channels.ServerSocketChannel

class TestServer {
    @Test
    fun `test server`() = runBlocking {

        val server = Server()
        server.handler { client, packet ->
            server.send(addr, packet)
            when (it) {
                is TestClient.DummyPacket -> {
                    it.a
                }
            }
        }
        client.registerType(TestClient.DummyPacket)
        client.start(DummyTransport.Client)
        client.transmit {
            sendPacket(TestClient.DummyPacket())
        }
    }
}
