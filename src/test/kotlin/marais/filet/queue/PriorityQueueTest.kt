package marais.filet.queue

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PriorityQueueTest {

    @Test
    fun testQueue() = runBlocking {
        val queue = PriorityQueue<String>()
        queue.send("C", 1)
        queue.send("A", 3)
        queue.send("D", 0)
        queue.send("A2", 3)
        queue.send("B", 2)

        assertEquals("A", queue.receive())
        assertEquals("A2", queue.receive())
        assertEquals("B", queue.receive())
        assertEquals("C", queue.receive())
        assertEquals("D", queue.receive())
    }

    @Test
    fun `test multi producers`() = runBlocking {
        val queue = PriorityQueue<String>()
        repeat(4) {
            launch(newFixedThreadPoolContext(4, "test")) {
                println("Hello from $it, ${Thread.currentThread().name}")
                repeat(4) { inside ->
                    queue.send("$it:$inside", it)
                }
            }
        }
        delay(500)
        // TODO
        while (!queue.empty) {
            println("received : ${queue.receive()}")
        }
    }
}
