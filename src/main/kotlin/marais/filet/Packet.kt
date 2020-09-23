package marais.filet

import okio.ByteString.Companion.encodeUtf8
import java.nio.ByteBuffer

abstract class Packet(val packetId: Byte, val priority: Int = 0) {

    var transmission: Int? = null

    /**
     * @return the number of bytes written
     */
    fun writeTo(buffer: ByteBuffer): Int {
        buffer.putInt(transmission!!)
        buffer.put(packetId)
        buffer.position(buffer.position() + 4)
        // Write packet data
        // This also returns the data length
        val size = serializeData(buffer)
        // Return to mark and put size
        buffer.putInt(5, size)
        return 4 + 1 + size
    }

    /**
     * @return the number of bytes written
     */
    protected abstract fun serializeData(buffer: ByteBuffer): Int

    companion object {
        const val MAX_PACKET_SIZE = 8192
    }
}

abstract class PacketReader<out T : Packet>(private val parser: ByteBuffer.() -> T) {

    fun read(buffer: ByteBuffer): T = parser(buffer)

    companion object {
        //inline fun <reified T : Packet> read(input: InputStream): T = Packet from input
    }
}
