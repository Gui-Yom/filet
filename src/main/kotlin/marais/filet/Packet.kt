package marais.filet

import okio.ByteString.Companion.encodeUtf8
import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer

abstract class Packet(val packetId: Byte, val priority: Int = 0) {

    var transmission: Int? = null

    /**
     * @return the number of bytes written
     */
    fun writeTo(buffer: ByteBuffer): Int {
        buffer.putInt(transmission!!)
        buffer.put(packetId)
        return 4 + 1 + serializeData(buffer)
    }

    /**
     * @return the number of bytes written
     */
    protected abstract fun serializeData(buffer: ByteBuffer): Int

    companion object {
        const val MAX_PACKET_SIZE = 8192
    }
}

abstract class PacketReader<out T : Packet>(private val parser: DataInputStream.() -> T) {

    fun read(input: InputStream): T = parser(DataInputStream(input))

    companion object {
        //inline fun <reified T : Packet> read(input: InputStream): T = Packet from input
    }
}

class TextPacket(val text: String) : Packet(1) {
    override fun serializeData(buffer: ByteBuffer): Int {
        val buf = text.encodeUtf8().toByteArray()
        buffer.put(buf)
        return buf.size
    }

    companion object Reader : PacketReader<TextPacket>({
        TextPacket(readUTF())
    })
}
