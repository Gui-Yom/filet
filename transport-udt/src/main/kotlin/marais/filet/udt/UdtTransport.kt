package marais.filet.udt

import marais.filet.transport.ClientTransport
import marais.filet.transport.ServerTransport
import udt.UDTClient
import udt.UDTServerSocket
import udt.UDTSocket
import java.net.InetAddress
import java.nio.ByteBuffer

object UdtTransport {

    class Client(val addr: InetAddress?, val port: Int?) : ClientTransport {

        internal constructor(remote: UDTSocket) : this(null, null) {
            this.remote = remote
        }

        private var client: UDTClient? = null
        private var remote: UDTSocket? = null

        override val localAddr: String
            get() = TODO("Not yet implemented")
        override val remoteAddr: String
            get() = TODO("Not yet implemented")

        override suspend fun init() {
            // We're not a remote socket
            if (remote == null) {
                requireNotNull(addr)
                requireNotNull(port)
                client = UDTClient(addr, port)
            }
        }

        override suspend fun writeBytes(buffer: ByteBuffer) {
            if (client != null) {

            }
        }

        override suspend fun readBytes(buffer: ByteBuffer): Int {
            TODO("Not yet implemented")
        }

        override fun close() {
            if (client != null) {
                client!!.shutdown()
            } else {
                remote!!.close()
            }
        }
    }

    class Server(val addr: InetAddress, val port: Int) : ServerTransport {

        private var serverSocket: UDTServerSocket? = null
        override val localAddr: String
            get() = "udt:${serverSocket!!.endpoint.localAddress}"

        override suspend fun init() {
            serverSocket = UDTServerSocket(addr, port)
        }

        override suspend fun accept(): ClientTransport {
            return Client(serverSocket!!.accept())
        }

        override fun close() {
            serverSocket!!.shutDown()
        }
    }
}
