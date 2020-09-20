package marais.filet.pipeline.impl

import marais.filet.Packet
import marais.filet.pipeline.Module

object SoutModule : Module {
    override fun processIn(packet: Packet): Packet {
        println("received : ${packet.packetId}, ${packet.priority}")
        return packet
    }

    override fun processOut(packet: Packet): Packet {
        return packet
    }
}