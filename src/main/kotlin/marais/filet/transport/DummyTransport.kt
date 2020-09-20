package marais.filet.transport

import marais.filet.Packet

object DummyTransport {
    object Client : ClientTransport {
        override suspend fun writePacket(packet: Packet) {
        }

        override suspend fun readPacket(): Packet {
            return object : Packet(-1) {}
        }

        override fun close() {
        }
    }
}