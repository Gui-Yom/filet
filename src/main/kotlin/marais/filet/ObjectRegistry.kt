package marais.filet

import kotlin.reflect.KClass

typealias RegistryEntry = Triple<KClass<out Any>, PacketId, Int>

class ObjectRegistry(entries: List<RegistryEntry>) {

    constructor(vararg entries: RegistryEntry) : this(listOf(*entries))

    constructor(vararg classes: KClass<out Any>) : this(classes.mapIndexed { idx, clazz ->
        RegistryEntry(clazz, idx.toByte(), 0)
    })

    private val registry = entries.toMutableList()

    operator fun get(clazz: KClass<out Any>): PacketId? = registry.find { it.first == clazz }?.second

    operator fun get(identifier: PacketId): KClass<*>? = registry.find { it.second == identifier }?.first

    fun getPriority(clazz: KClass<out Any>): Int? = registry.find { it.first == clazz }?.third

    fun getPriority(identifier: PacketId): Int? = registry.find { it.second == identifier }?.third
}
