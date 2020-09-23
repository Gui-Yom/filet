package marais.filet.transport.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import marais.filet.transport.ClientTransport
import marais.filet.transport.ServerTransport
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Initializing the class will effectively init the connection
 */
object TcpTransport {
    class Client(private val channel: AsynchronousSocketChannel, private val addr: SocketAddress? = null) : ClientTransport {

        constructor(addr: SocketAddress) : this(AsynchronousSocketChannel.open(), addr)

        constructor(channel: AsynchronousSocketChannel) : this(channel, null) {
            require(channel.isOpen)
            require(channel.remoteAddress != null)
        }

        init {
            channel.setOption(StandardSocketOptions.SO_SNDBUF, 8192)
            channel.setOption(StandardSocketOptions.SO_RCVBUF, 8192)
        }

        override suspend fun init() {
            require(channel.isOpen)
            if (channel.remoteAddress == null) {
                require(addr != null)
                channel.connect0(addr)
            }
        }

        override suspend fun writeBytes(buffer: ByteBuffer) {
            channel.write0(buffer)
        }

        override suspend fun readBytes(buffer: ByteBuffer): Int {
            return channel.read0(buffer)
        }

        override suspend fun readByte(): Byte {
            val buffer = ByteBuffer.allocate(1)
            channel.read0(buffer)
            return buffer.get()
        }

        override fun close() {
            channel.close()
        }

        suspend fun AsynchronousSocketChannel.read0(buffer: ByteBuffer): Int = suspendCoroutine { continuation ->
            read(buffer, continuation, object : CompletionHandler<Int, Continuation<Int>> {
                override fun completed(result: Int, attachment: Continuation<Int>) {
                    attachment.resume(result)
                }

                override fun failed(exc: Throwable, attachment: Continuation<Int>) {
                    attachment.resumeWithException(exc)
                }
            })
        }

        suspend fun AsynchronousSocketChannel.write0(buffer: ByteBuffer): Int = suspendCoroutine { continuation ->
            write(buffer, continuation, object : CompletionHandler<Int, Continuation<Int>> {
                override fun completed(result: Int, attachment: Continuation<Int>) {
                    attachment.resume(result)
                }

                override fun failed(exc: Throwable, attachment: Continuation<Int>) {
                    attachment.resumeWithException(exc)
                }
            })
        }

        suspend fun AsynchronousSocketChannel.connect0(addr: SocketAddress): Unit = suspendCoroutine { continuation ->
            connect(addr, continuation, object : CompletionHandler<Void, Continuation<Unit>> {
                override fun completed(result: Void, attachment: Continuation<Unit>) {
                    attachment.resume(Unit)
                }

                override fun failed(exc: Throwable, attachment: Continuation<Unit>) {
                    attachment.resumeWithException(exc)
                }
            })
        }
    }

    class Server(val addr: SocketAddress) : ServerTransport {

        val server = AsynchronousServerSocketChannel.open()

        override suspend fun init() {
            withContext(Dispatchers.IO) {
                server.bind(addr, 4)
            }
        }

        override suspend fun accept(): ClientTransport {
            return Client((server.accept() as CompletableFuture).minimalCompletionStage().await())
        }

        override fun close() {
            server.close()
        }
    }
}
