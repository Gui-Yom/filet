package marais.filet

import kotlin.reflect.KClass

/**
 * A registry entry, in order :
 * the registered class, the corresponding packet id, the default priority for this object.
 */
typealias RegistryEntry = Triple<KClass<out Any>, PacketId, Int>

/**
 * The registry containing the triplets : (Class, PacketId, Priority).
 * It is automatically populated when using a [DefaultGlobalSerializer].
 *
 * This constructor initialize a [ClassRegistry] based on a set of registry entries.
 */
class ClassRegistry(entries: List<RegistryEntry>) {

    /**
     * Initialize a [ClassRegistry] based on a set of registry entries.
     */
    constructor(vararg entries: RegistryEntry) : this(listOf(*entries))

    /**
     * Initialize a [ClassRegistry] based on a set of classes.
     * Their packet id will automatically be generated.
     * Their priority will all be set to 0.
     */
    constructor(vararg classes: KClass<out Any>) : this(classes.mapIndexed { idx, clazz ->
        RegistryEntry(clazz, idx.toByte(), 0)
    })

    constructor() : this(listOf())

    /**
     * True if this registry is populated automatically.
     */
    var automatic = false

    private val registry = entries.toMutableList()

    operator fun get(clazz: KClass<out Any>): PacketId? = registry.find { it.first == clazz }?.second

    operator fun get(identifier: PacketId): KClass<*>? = registry.find { it.second == identifier }?.first

    fun getPriority(clazz: KClass<out Any>): Int? = registry.find { it.first == clazz }?.third

    fun getPriority(identifier: PacketId): Int? = registry.find { it.second == identifier }?.third

    /**
     * Add new classes to the registry.
     */
    fun register(vararg entries: RegistryEntry) {
        for (entry in entries) {
            if (registry.find { it.first == entry.first } != null)
                throw IllegalArgumentException("Class '${entry.first.qualifiedName}' has already been registered.")
            if (registry.find { it.second == entry.second } != null)
                throw IllegalArgumentException("Packet identifier '${entry.second}' has already been registered.")
            registry.add(entry)
        }
    }

    /**
     * Add new classes to the registry.
     * The packet id will automatically be generated based on the last registry entry.
     * Their priority will all be set to 0.
     */
    fun register(vararg classes: KClass<out Any>) {
        var packetId = registry.lastOrNull()?.second ?: -1
        for (clazz in classes) {
            if (registry.find { it.first == clazz } != null)
                throw IllegalArgumentException("Class '${clazz.qualifiedName}' has already been registered.")
            registry.add(RegistryEntry(clazz, ++packetId, 0))
        }
    }
}
