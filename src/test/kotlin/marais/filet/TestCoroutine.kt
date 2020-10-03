package marais.filet

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class TestCoroutine {
    @Test
    fun `test coroutines`() = runBlocking {
        println("a")
        launch {
            delay(1000)
            println("b")
        }
        println("c")
    }
}