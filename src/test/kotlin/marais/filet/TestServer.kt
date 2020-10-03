package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.transport.impl.DummyTransport
import org.junit.jupiter.api.Test

class TestServer {
    @Test
    fun `test server`() = runBlocking {

        val server = Server(this)
        server.handler { server, obj ->
            server.clients.forEach {
                it.transmit {
                    sendPacket(obj)
                }
            }
        }
        server.registerSerializer(TestClient.DummyPacket)
        server.start(DummyTransport.Server)
    }
}
