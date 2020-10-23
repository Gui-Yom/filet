package marais.filet

import kotlin.reflect.KClass

/**
 * Runtime error that indicates we can't correctly (de)serialize an object because its class is not registered.
 */
class ClassUnregisteredException(message: String) : RuntimeException(message) {

    /**
     * Used for [PacketSerializer.write], when guessing the [PacketSerializer] based on the object type.
     *
     * @param clazz the class of the packet we couldn't find a [PacketSerializer] for.
     */
    constructor(clazz: KClass<*>) : this("Class '${clazz.qualifiedName}' hasn't been registered.")

    /**
     * Used for [PacketSerializer.read], when guessing the [PacketSerializer] based on the packetType byte.
     *
     * @param packetType the packetType byte of the packet we couldn't find a [PacketSerializer] for.
     */
    constructor(packetType: PacketId) : this("Packet identifier '$packetType' is unknown.")
}
