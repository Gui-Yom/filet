package marais.filet

import java.io.DataOutputStream
import java.io.OutputStream

abstract class Packet(val packetId: Byte, val priority: Int = 0) {

    var transmission: Int? = null

    fun writeTo(output: OutputStream) = writeTo(DataOutputStream(output))

    fun writeTo(output: DataOutputStream) {
        output.writeInt(transmission!!)
        output.writeByte(packetId.toInt())
        serializeData(output)
        output.flush()
    }

    protected abstract fun serializeData(output: DataOutputStream)
}
