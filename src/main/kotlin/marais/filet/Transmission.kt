package marais.filet

/**
 * An ephemeral stream bonding related packets together.
 * Use [sendPacket] to send a packet through this transmission.
 */
interface Transmission {
    val transmitId: Int

    /**
     * Send packets through this transmission.
     */
    fun sendPacket(obj: Any, priority: Int = -1)
}
