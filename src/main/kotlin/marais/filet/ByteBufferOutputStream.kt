package marais.filet

import java.io.OutputStream
import java.nio.ByteBuffer

class ByteBufferOutputStream(val buffer: ByteBuffer) : OutputStream() {
    override fun write(b: Int) {
        buffer.put(b.toByte())
    }
}
