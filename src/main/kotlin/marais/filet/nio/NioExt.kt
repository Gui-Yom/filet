package marais.filet.nio

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Performs [AsynchronousServerSocketChannel.accept] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
internal suspend fun AsynchronousServerSocketChannel.aAccept() =
    suspendCancellableCoroutine<AsynchronousSocketChannel> { cont ->
        accept(cont, asyncIOHandler())
        closeOnCancel(cont)
    }

/**
 * Performs [AsynchronousSocketChannel.connect] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
internal suspend fun AsynchronousSocketChannel.aConnect(
    socketAddress: SocketAddress
) = suspendCancellableCoroutine<Unit> { cont ->
    connect(socketAddress, cont, AsyncVoidIOHandler)
    closeOnCancel(cont)
}

/**
 * Performs [AsynchronousSocketChannel.read] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
internal suspend fun AsynchronousSocketChannel.aRead(
    buf: ByteBuffer,
    timeout: Long = 0L,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) = suspendCancellableCoroutine<Int> { cont ->
    read(buf, timeout, timeUnit, cont, asyncIOHandler())
    closeOnCancel(cont)
}

/**
 * Performs [AsynchronousSocketChannel.write] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
internal suspend fun AsynchronousSocketChannel.aWrite(
    buf: ByteBuffer,
    timeout: Long = 0L,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) = suspendCancellableCoroutine<Int> { cont ->
    write(buf, timeout, timeUnit, cont, asyncIOHandler())
    closeOnCancel(cont)
}

// ---------------- private details ----------------

private fun Channel.closeOnCancel(cont: CancellableContinuation<*>) {
    cont.invokeOnCancellation {
        try {
            close()
        } catch (ex: Throwable) {
            // Specification says that it is Ok to call it any time, but reality is different,
            // so we have just to ignore exception
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> asyncIOHandler(): CompletionHandler<T, CancellableContinuation<T>> =
    AsyncIOHandlerAny as CompletionHandler<T, CancellableContinuation<T>>

private object AsyncIOHandlerAny : CompletionHandler<Any, CancellableContinuation<Any>> {
    override fun completed(result: Any, cont: CancellableContinuation<Any>) {
        cont.resume(result)
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Any>) {
        // just return if already cancelled and got an expected exception for that case
        if (ex is AsynchronousCloseException && cont.isCancelled) return
        cont.resumeWithException(ex)
    }
}

private object AsyncVoidIOHandler : CompletionHandler<Void?, CancellableContinuation<Unit>> {
    override fun completed(result: Void?, cont: CancellableContinuation<Unit>) {
        cont.resume(Unit)
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Unit>) {
        // just return if already cancelled and got an expected exception for that case
        if (ex is AsynchronousCloseException && cont.isCancelled) return
        cont.resumeWithException(ex)
    }
}
