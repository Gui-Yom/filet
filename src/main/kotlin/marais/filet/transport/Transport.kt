package marais.filet.transport

import java.io.Closeable
import java.nio.ByteBuffer

interface ClientTransport : Closeable {

    suspend fun init()

    /**
     * Write up to buffer.limit() bytes from the buffer
     */
    suspend fun writeBytes(buffer: ByteBuffer)

    /**
     * Reads up to buffer.limit() bytes
     */
    suspend fun readBytes(buffer: ByteBuffer): Int

    /**
     * Reads one byte
     */
    suspend fun readByte(): Byte
}

interface ServerTransport : Closeable {

    suspend fun init()

    suspend fun accept(): ClientTransport
}
