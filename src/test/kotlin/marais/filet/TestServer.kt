package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.pipeline.Pipeline
import marais.filet.transport.impl.DummyTransport
import org.junit.jupiter.api.Test

class TestServer {
    @Test
    fun `test server`() = runBlocking {

        val server = Server(this, Pipeline(DefaultPacketSerializer(TestClient.DummyPacket)))
        server.handler { client, obj ->
            server.broadcast(obj)
        }
        server.start(DummyTransport.Server)
    }
}
