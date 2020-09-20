package marais.filet.transport

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import marais.filet.Packet
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Initializing the class will effectively init the connection
 */
object TcpTransport {
    class Client(val addr: SocketAddress) : ClientTransport {

        val channel = AsynchronousSocketChannel.open()

        init {
            channel.setOption(StandardSocketOptions.SO_SNDBUF, 8192)
            channel.setOption(StandardSocketOptions.SO_RCVBUF, 8192)
            CoroutineScope(CoroutineName("ClientInit") + Dispatchers.Default).launch {
                channel.connect0(addr)
            }
        }

        override fun close() {
            channel.close()
        }

        override suspend fun writePacket(packet: Packet): Unit {
            //channel.write0(packet.serialize())
        }

        override suspend fun readPacket(): Packet {
            TODO("Not yet implemented")
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
            connect(addr, continuation, DefaultCompletionHandler as CompletionHandler<Void, in Continuation<Unit>>)
        }

        private object DefaultCompletionHandler : CompletionHandler<Any, Continuation<Any>> {
            override fun completed(result: Any, attachment: Continuation<Any>) {
                attachment.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Continuation<Any>) {
                attachment.resumeWithException(exc)
            }

        }
    }

    class Server : ServerTransport {

        val server = AsynchronousServerSocketChannel.open()

        override fun close() {
            TODO("Not yet implemented")
        }
    }
}
