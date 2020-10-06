package marais.filet.ktor

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import marais.filet.transport.ClientTransport
import marais.filet.transport.ServerTransport
import java.nio.ByteBuffer

object KtorTransport {
    class Client(private val socketBuilder: TcpSocketBuilder?, private val addr: NetworkAddress? = null) :
        ClientTransport {

        @KtorExperimentalAPI
        constructor(addr: NetworkAddress) : this(aSocket(ActorSelectorManager(Dispatchers.Default)).tcp(), addr)

        constructor(socket: Socket) : this(null, null) {
            this.socket = socket
        }

        private var socket: Socket? = null
        private var readChannel: ByteReadChannel? = null
        private var writeChannel: ByteWriteChannel? = null

        override val localAddr: String
            get() = "ktor:${socket!!.localAddress}"
        override val remoteAddr: String
            get() = "ktor:${socket!!.remoteAddress}"

        @KtorExperimentalAPI
        override suspend fun init() {
            if (socket == null) {
                require(addr != null)
                val configure = fun SocketOptions.TCPClientSocketOptions.(): Unit {
                    // TODO socket options
                }
                socket = socketBuilder?.connect(addr, configure)
                    ?: aSocket(ActorSelectorManager(Dispatchers.Default)).tcp().connect(addr, configure)
            }
            writeChannel = socket!!.openWriteChannel(true)
            readChannel = socket!!.openReadChannel()
        }

        override suspend fun writeBytes(buffer: ByteBuffer) {
            writeChannel!!.writeFully(buffer)
        }

        @ExperimentalIoApi
        override suspend fun readBytes(buffer: ByteBuffer): Int {
            val size = buffer.remaining()
            readChannel!!.read(size) { mem, start, end ->
                require(end - start >= size)
                mem.copyTo(buffer, start)
                size
            }
            return size
        }

        override fun close() {
            socket?.close()
        }
    }

    class Server(val addr: NetworkAddress) : ServerTransport {

        private var serverSocket: ServerSocket? = null
        override val localAddr: String
            get() = "ktor:${serverSocket!!.localAddress}"

        @KtorExperimentalAPI
        override suspend fun init() {
            serverSocket = aSocket(ActorSelectorManager(Dispatchers.Default)).tcp().bind(addr) {

            }
        }

        override suspend fun accept(): ClientTransport = Client(serverSocket!!.accept())

        override fun close() {
            serverSocket?.close()
        }
    }
}
