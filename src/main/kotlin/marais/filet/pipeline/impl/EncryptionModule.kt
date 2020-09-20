package marais.filet.pipeline.impl

import marais.filet.Packet
import marais.filet.pipeline.Module
import java.io.DataOutputStream
import java.nio.ByteBuffer

class EncryptionModule : Module {

    override fun processIn(packet: Packet): Packet {
        return packet
    }

    override fun processOut(packet: Packet): Packet {
        return EncryptedPacket(packet)
    }

    class EncryptedPacket(packet: Packet) : Packet(-1) {
        override fun serializeData(output: DataOutputStream) {
            val buffer = ByteBuffer.allocate(1000000)
            //
            output.writeInt(buffer.position())
            output.write(buffer.array(), 0, buffer.position())
        }
    }

    companion object Reader
}
