package marais.filet.transport.impl

import marais.filet.transport.ClientTransport
import marais.filet.transport.ServerTransport
import java.nio.ByteBuffer

object DummyTransport {
    object Client : ClientTransport {

        override val localAddr: String
            get() = "dummy:0"
        override val remoteAddr: String
            get() = "dummy:0"

        override suspend fun init() {}

        override suspend fun writeBytes(buffer: ByteBuffer) {}

        override suspend fun readBytes(buffer: ByteBuffer): Int {
            return 0
        }

        override fun close() {}
    }

    object Server : ServerTransport {

        override val localAddr: String
            get() = "dummy:0"

        override suspend fun init() {}

        override suspend fun accept(): ClientTransport {
            return Client
        }

        override fun close() {}
    }
}