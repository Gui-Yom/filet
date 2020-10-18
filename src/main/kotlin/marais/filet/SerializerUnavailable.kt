package marais.filet

import kotlin.reflect.KClass

/**
 * Runtime error that indicates no [GlobalPacketSerializer] has been registered to handle a given packet.
 */
class SerializerUnavailable(message: String) : RuntimeException(message) {

    /**
     * Used for [GlobalPacketSerializer.write], when guessing the [GlobalPacketSerializer] based on the object type.
     *
     * @param clazz the class of the packet we couldn't find a [GlobalPacketSerializer] for.
     */
    constructor(clazz: KClass<*>) : this("No PacketSerializer instance is available for class: ${clazz.qualifiedName}")

    /**
     * Used for [GlobalPacketSerializer.read], when guessing the [GlobalPacketSerializer] based on the packetType byte.
     *
     * @param packetType the packetType byte of the packet we couldn't find a [GlobalPacketSerializer] for.
     */
    constructor(packetType: PacketId) : this("No PacketSerializer instance is available for packetType: $packetType")
}
