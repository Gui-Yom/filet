package marais.filet.transport

import kotlinx.coroutines.channels.Channel
import marais.filet.Packet

object EchoTransport {
    class Client : ClientTransport {

        val channel = Channel<Packet>(Channel.Factory.UNLIMITED)

        override suspend fun writePacket(packet: Packet) {
            channel.send(packet)
        }

        override suspend fun readPacket(): Packet {
            return channel.receive()
        }

        override fun close() {
            channel.close()
        }
    }

    /*
    class Server() : ServerTransport {

    }

     */
}