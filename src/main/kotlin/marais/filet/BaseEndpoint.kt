package marais.filet

import marais.filet.pipeline.Pipeline
import java.io.Closeable

/**
 * Holds common tools like serializers and modules.
 */
abstract class BaseEndpoint internal constructor(
    val pipeline: Pipeline
) : Closeable {

    protected val registry = pipeline.serializer.registry

    /**
     * true if the endpoint is closed
     */
    protected var isClosed = false

    override fun close() {
        isClosed = true
    }
}
