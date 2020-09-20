package marais.filet.pipeline.impl

import marais.filet.Packet
import marais.filet.pipeline.Module
import java.util.*

class Pipeline(modules: List<Module>) : Module {

    constructor(vararg module: Module) : this(listOf(*module))

    /**
     * IN direction
     */
    private val modules = LinkedList(modules)

    override fun processIn(packet: Packet): Packet {
        var p = packet
        modules.forEach {
            p = it.processIn(p)
        }
        return p
    }

    override fun processOut(packet: Packet): Packet {
        var p = packet
        modules.asReversed().forEach {
            p = it.processOut(p)
        }
        return p
    }
}