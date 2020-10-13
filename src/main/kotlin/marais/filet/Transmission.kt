package marais.filet

/**
 * An ephemeral stream bonding related packets together.
 * Use [send] to send a packet through this transmission.
 */
interface Transmission {
    val transmitId: Int

    /**
     * Send packets through this transmission.
     */
    fun send(obj: Any, priority: Int = -1)
}
