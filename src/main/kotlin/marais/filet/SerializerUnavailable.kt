package marais.filet

import kotlin.reflect.KClass

/**
 * Runtime error that indicates no [PacketSerializer] has been registered to handle a given packet.
 */
class SerializerUnavailable(message: String) : RuntimeException(message) {

    /**
     * Used for [PacketSerializer.write], when guessing the [PacketSerializer] based on the object type.
     *
     * @param clazz the class of the packet we couldn't find a [PacketSerializer] for.
     */
    constructor(clazz: KClass<*>) : this("No PacketSerializer instance is available for class: ${clazz.qualifiedName}")

    /**
     * Used for [PacketSerializer.read], when guessing the [PacketSerializer] based on the packetType byte.
     *
     * @param packetType the packetType byte of the packet we couldn't find a [PacketSerializer] for.
     */
    constructor(packetType: Byte) : this("No PacketSerializer instance is available for packetType: $packetType")
}
