package marais.filet.transport.impl

import kotlinx.coroutines.channels.Channel
import marais.filet.transport.ClientTransport
import marais.filet.transport.ServerTransport
import java.nio.ByteBuffer

object EchoTransport {
    class Client : ClientTransport {

        val channel = Channel<Any>(Channel.Factory.UNLIMITED)

        override suspend fun init() {
            TODO("Not yet implemented")
        }

        override suspend fun writeBytes(buffer: ByteBuffer) {
            TODO("Not yet implemented")
        }

        override suspend fun readBytes(buffer: ByteBuffer): Int {
            TODO("Not yet implemented")
        }

        override fun close() {
            channel.close()
        }
    }

    class Server : ServerTransport {
        override suspend fun init() {
            TODO("Not yet implemented")
        }

        override suspend fun accept(): ClientTransport {
            TODO("Not yet implemented")
        }

        override fun close() {
            TODO("Not yet implemented")
        }
    }
}