package marais.filet

import java.nio.ByteBuffer

abstract class PacketSerializer<T>(val packetId: Byte, val priority: Int = 0) {

    /**
     * @return the number of bytes written
     */
    fun write(transmission: Int, obj: T, out: ByteBuffer): Int {
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

    abstract fun read(buffer: ByteBuffer): T

    abstract fun getPacketClass(): Class<T>
}
