package marais.filet

import java.nio.ByteBuffer

interface PacketSerializer<T> {

    val packetId: Byte

    val priority: Int

    fun write(transmission: Int, obj: T, out: ByteBuffer): Int

    fun read(buffer: ByteBuffer): T

    fun getPacketClass(): Class<T>
}

abstract class AbstractPacketSerializer<T>(override val packetId: Byte, override val priority: Int = 0) : PacketSerializer<T> {

    /**
     * @return the number of bytes written
     */
    override fun write(transmission: Int, obj: T, out: ByteBuffer): Int {
        out.putInt(transmission)
        out.put(packetId)
        out.position(out.position() + 4)
        // Write packet data
        // This also returns the data length
        val size = writeData(obj, out)
        // Return to mark and put size
        out.putInt(5, size)
        return 4 + 1 + size
    }

    /**
     * Override this method to write your own data to the packet. The packet header is already written.
     *
     * @return the number of bytes written
     */
    protected abstract fun writeData(obj: T, buffer: ByteBuffer): Int
}
