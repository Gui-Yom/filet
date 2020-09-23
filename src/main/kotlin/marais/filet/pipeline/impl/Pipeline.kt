package marais.filet.pipeline.impl

import marais.filet.Packet
import marais.filet.pipeline.Module
import java.nio.ByteBuffer
import java.util.*

class Pipeline(modules: List<Module>) : Module {

    constructor(vararg module: Module) : this(listOf(*module))

    /**
     * IN direction
     */
    private val modules = LinkedList(modules)

    override fun processIn(packet: Packet, buf: ByteBuffer): Pair<Packet, ByteBuffer> {
        var p = packet to buf
        modules.forEach {
            p = it.processIn(p.first, p.second)
        }
        return p
    }

    override fun processOut(packet: Packet, buf: ByteBuffer): Pair<Packet, ByteBuffer> {
        var p = packet to buf
        modules.asReversed().forEach {
            p = it.processOut(p.first, p.second)
        }
        return p
    }
}
