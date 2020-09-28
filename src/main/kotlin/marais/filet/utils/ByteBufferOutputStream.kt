package marais.filet.utils

import java.io.OutputStream
import java.nio.ByteBuffer

class ByteBufferOutputStream(val buffer: ByteBuffer) : OutputStream() {
    override fun write(b: Int) {
        buffer.put(b.toByte())
    }
}
