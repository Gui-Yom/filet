package marais.filet.pipeline

import marais.filet.Packet
import java.nio.ByteBuffer

interface Module {
    fun processIn(packet: Packet, buf: ByteBuffer): Pair<Packet, ByteBuffer>

    fun processOut(packet: Packet, buf: ByteBuffer): Pair<Packet, ByteBuffer>
}
