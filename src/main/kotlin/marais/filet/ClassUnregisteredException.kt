package marais.filet

import kotlin.reflect.KClass

/**
 * Runtime error that indicates we can't correctly (de)serialize an object because its class is not registered.
 */
class ClassUnregisteredException(message: String) : RuntimeException(message) {

    /**
     * Used for [GlobalPacketSerializer.write], when guessing the [GlobalPacketSerializer] based on the object type.
     *
     * @param clazz the class of the packet we couldn't find a [GlobalPacketSerializer] for.
     */
    constructor(clazz: KClass<*>) : this("Class '${clazz.qualifiedName}' hasn't been registered.")

    /**
     * Used for [GlobalPacketSerializer.read], when guessing the [GlobalPacketSerializer] based on the packetType byte.
     *
     * @param packetType the packetType byte of the packet we couldn't find a [GlobalPacketSerializer] for.
     */
    constructor(packetType: PacketId) : this("Packet identifier '$packetType' isn't registered to a class.")
}
