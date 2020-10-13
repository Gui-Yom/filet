package marais.filet.transport.impl

import marais.filet.transport.ClientTransport
import marais.filet.transport.ServerTransport
import marais.filet.utils.aAccept
import marais.filet.utils.aConnect
import marais.filet.utils.aRead
import marais.filet.utils.aWrite
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel

/**
 * Initializing the class will effectively init the connection
 */
object TcpTransport {
    class Client(private val channel: AsynchronousSocketChannel, var addr: SocketAddress? = null) :
        ClientTransport {

        constructor(addr: SocketAddress) : this(AsynchronousSocketChannel.open(), addr)

        constructor(channel: AsynchronousSocketChannel) : this(channel, null) {
            require(channel.isOpen)
            require(channel.remoteAddress != null)
        }

        override val localAddr: String
            get() = "tcp:${channel.localAddress}"
        override val remoteAddr: String
            get() = "tcp:${channel.remoteAddress}"

        override suspend fun init() {
            require(channel.isOpen)
            if (channel.remoteAddress == null) {
                require(addr != null)
                channel.setOption(StandardSocketOptions.SO_SNDBUF, 32768)
                channel.setOption(StandardSocketOptions.SO_RCVBUF, 32768)
                channel.aConnect(addr!!)
            } else {
                addr = channel.remoteAddress
            }
        }

        override suspend fun writeBytes(buffer: ByteBuffer) {
            channel.aWrite(buffer)
        }

        override suspend fun readBytes(buffer: ByteBuffer): Int {
            return channel.aRead(buffer)
        }

        override fun close() {
            channel.close()
        }
    }

    class Server(val addr: SocketAddress?) : ServerTransport {

        val server = AsynchronousServerSocketChannel.open()

        override val localAddr: String
            get() = "tcp:${server.localAddress}"

        override suspend fun init() {
            server.bind(addr, 4)
        }

        override suspend fun accept(): ClientTransport {
            return Client(server.aAccept())
        }

        override fun close() {
            server.close()
        }
    }
}
