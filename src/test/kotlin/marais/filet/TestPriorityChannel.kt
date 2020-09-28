package marais.filet

import kotlinx.coroutines.runBlocking
import marais.filet.utils.PriorityChannel
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

object TestPriorityChannel {

    // TODO parameterize
    @Test
    fun `queue should maintain priority order`() = runBlocking {
        val queue = PriorityChannel<Int>(Comparator.comparingInt { it })
        queue.send(3)
        queue.send(6)
        queue.send(2)
        assertEquals(2, queue.receive())
        assertEquals(3, queue.receive())
    }

    @Test
    fun `queue operation should not block`() = runBlocking {
        // TODO
    }
}
