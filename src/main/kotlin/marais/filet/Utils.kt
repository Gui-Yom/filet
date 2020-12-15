package marais.filet

import java.nio.ByteBuffer

fun ByteBuffer?.contentToString(): String {
    if (this == null) return "null"
    val iMax: Int = remaining() - 1
    if (iMax == -1) return "[]"

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(get(i))
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}
