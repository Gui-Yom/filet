package marais.filet.pipeline

import marais.filet.Packet

interface Module {
    fun processIn(packet: Packet): Packet

    fun processOut(packet: Packet): Packet
}
