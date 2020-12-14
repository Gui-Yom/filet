package marais.filet.queue

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Unbounded MPSC stable priority queue based on a pairing heap.
 */
class PriorityQueue<E> {
    private var root: HeapNode? = null
    private var seq = 0

    val empty: Boolean
        get() = root == null

    private val top: E?
        get() = root?.value

    private val lock = Mutex()

    private val shortcutLock = Mutex()

    private var waitingForReceive: Continuation<E>? = null

    suspend fun send(value: E, priority: Int = 0) {

        // If a thread is waiting for some data, shortcut by giving it directly
        shortcutLock.withLock {
            if (waitingForReceive != null) {
                waitingForReceive!!.resume(value)
                return
            }
        }

        lock.withLock {
            insert(value, priority)
        }
    }

    suspend fun receive(): E {
        return lock.withLock {
            if (!empty) {
                val top = top!!
                deleteMax()
                return top
            } else {
                shortcutLock.lock()
                suspendCoroutine {
                    waitingForReceive = it

                    shortcutLock.unlock()
                    lock.unlock()
                }
            }
        }
    }

    private fun insert(data: E, priority: Int) {
        root = merge(root, HeapNode(data, priority))
    }

    private fun merge(node: HeapNode?, other: HeapNode?): HeapNode? {
        return if (node == null)
            other
        else if (other == null)
            node
        else if (node.priority > other.priority || (node.priority == other.priority && node.seq < other.seq)) {
            node.addChild(other)
            node
        } else {
            other.addChild(node)
            other
        }
    }

    private fun merge2Pass(node: HeapNode?): HeapNode? {
        return if (node?.right == null)
            node
        else {
            val old = node.right
            val new = node.right!!.right
            node.right!!.right = null
            node.right = null
            merge(merge(node, old), merge2Pass(new))
        }
    }

    private fun deleteMax() {
        // Delete the root node
        root = merge2Pass(root?.left)
    }

    inner class HeapNode(
        val value: E,
        val priority: Int,
        val seq: Int = this@PriorityQueue.seq++,
        var left: HeapNode? = null,
        var right: HeapNode? = null
    ) {

        fun addChild(node: HeapNode) {
            if (left == null)
                left = node
            else {
                node.right = left
                left = node
            }
        }
    }
}
