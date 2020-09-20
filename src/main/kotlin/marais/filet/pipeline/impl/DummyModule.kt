package marais.filet.pipeline.impl

import marais.filet.Packet
import marais.filet.pipeline.Module

object DummyModule : Module {
    override fun processIn(packet: Packet): Packet {
        return packet
    }

    override fun processOut(packet: Packet): Packet {
        return packet
    }
}
