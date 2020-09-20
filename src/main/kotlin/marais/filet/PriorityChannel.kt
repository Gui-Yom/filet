package marais.filet

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.selects.SelectClause2
import java.util.concurrent.PriorityBlockingQueue

internal class PriorityChannel<E>(comparator: Comparator<E>) : Channel<E> {

    private val queue = PriorityBlockingQueue(16, comparator)
    private var closed = false

    @ExperimentalCoroutinesApi
    override val isClosedForReceive: Boolean
        get() = closed && queue.size == 0

    @ExperimentalCoroutinesApi
    override val isClosedForSend: Boolean
        get() = closed

    @ExperimentalCoroutinesApi
    override val isEmpty: Boolean
        get() = queue.size == 0

    @ExperimentalCoroutinesApi
    override val isFull: Boolean
        get() = TODO("Not yet implemented")
    override val onReceive: SelectClause1<E>
        get() = TODO("Not yet implemented")

    @InternalCoroutinesApi
    override val onReceiveOrClosed: SelectClause1<ValueOrClosed<E>>
        get() = TODO("Not yet implemented")

    @ObsoleteCoroutinesApi
    override val onReceiveOrNull: SelectClause1<E?>
        get() = TODO("Not yet implemented")
    override val onSend: SelectClause2<E, SendChannel<E>>
        get() = TODO("Not yet implemented")

    override fun cancel(cause: Throwable?): Boolean {
        queue.clear()
        return close(cause)
    }

    override fun cancel(cause: CancellationException?) {
        close(cause)
        queue.clear()
    }

    override fun close(cause: Throwable?): Boolean {
        return if (closed)
            false
        else {
            closed = true
            true
        }
    }

    @ExperimentalCoroutinesApi
    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun iterator(): ChannelIterator<E> {
        TODO("Not yet implemented")
    }

    override fun offer(element: E): Boolean {
        return queue.offer(element)
    }

    override fun poll(): E? {
        return queue.poll()
    }

    override suspend fun receive(): E {
        var elem = poll()
        if (elem == null && closed)
            throw ClosedReceiveChannelException("No more elem while closed")
        if (elem != null)
            return elem

        elem = poll()
        while (elem == null) {
            elem = poll()
            yield()
        }
        return elem
    }

    @InternalCoroutinesApi
    override suspend fun receiveOrClosed(): ValueOrClosed<E> {
        TODO("Not yet implemented")
    }

    @ObsoleteCoroutinesApi
    override suspend fun receiveOrNull(): E? {
        TODO("Not yet implemented")
    }

    override suspend fun send(element: E) {
        queue.offer(element)
    }
}
