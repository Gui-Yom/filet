package marais.filet.transport

import java.io.Closeable
import java.nio.ByteBuffer

interface ClientTransport : Closeable {

    suspend fun writeBytes(buffer: ByteBuffer)

    suspend fun readBytes(buffer: ByteBuffer): Int
}

interface ServerTransport : Closeable {

}
