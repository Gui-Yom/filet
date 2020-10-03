package marais.filet

import kotlin.reflect.KClass

class SerializerUnavailable(message: String) : RuntimeException(message) {

    constructor(clazz: KClass<*>) : this("No PacketSerializer instance is available for class: ${clazz.qualifiedName}")

    constructor(packetType: Byte) : this("No PacketSerializer instance is available for packetType: $packetType")
}
