package marais.filet

import java.nio.ByteBuffer
import kotlin.reflect.KClass

abstract class PacketSerializer<T : Any>(val packetId: Byte, val priority: Int = 0) {

    /**
     * @return the number of bytes written
     */
    fun write(transmission: Int, obj: T, out: ByteBuffer): Int {
        out.putInt(transmission)
        out.put(packetId)
        out.position(9)
        // Write packet data
        // This also returns the data length
        val size = writeData(obj, out)
        // Return to mark and put size
        out.putInt(5, size)
        return 4 + 1 + 4 + size
    }

    /**
     * Override this method to write your own data to the packet. The packet header is already written.
     *
     * @return the number of bytes written
     */
    protected abstract fun writeData(obj: T, buffer: ByteBuffer): Int

    abstract fun read(buffer: ByteBuffer): T

    abstract fun getPacketKClass(): KClass<T>

    fun getPacketClass(): Class<T> {
        return getPacketKClass().java
    }

    companion object {
        const val MAX_PACKET_SIZE = 8192
    }
}
