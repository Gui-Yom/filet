package marais.filet.transport

import java.io.Closeable
import java.nio.ByteBuffer

/**
 * The underlying transport protocol used for a client.
 * The connection should not be opened until a call to [init].
 */
interface ClientTransport : Closeable {

    val localAddr: String

    val remoteAddr: String

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
     * Shutdown the transport and release any associated resource.
     */
    override fun close()
}

/**
 * The underlying transport protocol used for a server.
 * The server should not bind/listen for connections before a call to [init].
 */
interface ServerTransport : Closeable {

    val localAddr: String

    suspend fun init()

    /**
     * Accept a connection, suspending for one to become available.
     *
     * @return a reference to a [ClientTransport] of the same protocol representing a remote client.
     */
    suspend fun accept(): ClientTransport

    /**
     * Shutdown the transport and release any associated resource.
     */
    override fun close()
}
