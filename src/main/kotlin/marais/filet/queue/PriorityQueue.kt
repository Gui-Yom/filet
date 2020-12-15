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

    /**
     * The root node
     */
    private var root: HeapNode? = null

    /**
     * The counter to dissociate nodes based on their insertion order. Makes the queue stable.
     */
    private var seq = 0

    /**
     * True if this queue is empty
     */
    val empty: Boolean
        get() = root == null

    /**
     * The root node value
     */
    private val rootValue: E?
        get() = root?.value

    /**
     * Lock for every operations with this queue
     */
    private val lock = Mutex()

    /**
     * Lock for shortcut operations
     */
    private val shortcutLock = Mutex()

    /**
     * Non null when a coroutine is waiting for an item
     */
    private var waitingForReceive: Continuation<E>? = null

    /**
     * Post an item in this queue. If a coroutine is waiting for an item, the item is directly transferred to the other end.
     */
    suspend fun send(value: E, priority: Int = 0) {

        // If a receiver is waiting for some data, it means the queue is empty,
        // shortcut by transferring it directly
        shortcutLock.withLock {
            if (waitingForReceive != null) {
                waitingForReceive!!.resume(value)
                return
            }
        }

        lock.withLock {
            offer(value, priority)
        }
    }

    /**
     * Try to retrieve an item from this queue, or suspend until one arrives.
     */
    suspend fun receive(): E {
        return lock.withLock {
            if (!empty) {
                return poll()!!
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

    /**
     * Insert an item in the queue. This operation can't fail as this queue is unbounded.
     */
    private fun offer(data: E, priority: Int) {
        root = merge(root, HeapNode(data, priority))
    }

    /**
     * Try to retrieve an item from the queue or null.
     */
    private fun poll(): E? {
        val value = rootValue
        deleteRoot()
        return value
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

    /**
     * Remove the root node and rebuild the tree.
     */
    private fun deleteRoot() {
        // Delete the root node
        root = merge2Pass(root?.left)
    }

    private inner class HeapNode(
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
