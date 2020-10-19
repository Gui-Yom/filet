package marais.filet.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.fasterxml.jackson.databind.util.ByteBufferBackedOutputStream
import marais.filet.GlobalPacketSerializer
import marais.filet.ClassRegistry
import java.nio.ByteBuffer
import kotlin.reflect.KClass

class JacksonSerializer(val mapper: ObjectMapper, registry: ClassRegistry) : GlobalPacketSerializer(registry) {

    override fun <T : Any> deserialize(buffer: ByteBuffer, clazz: KClass<T>): T {
        return mapper.readValue(ByteBufferBackedInputStream(buffer), clazz.java)
    }

    override fun <T : Any> serialize(obj: T, clazz: KClass<T>, out: ByteBuffer) {
        mapper.writeValue(ByteBufferBackedOutputStream(out), obj)
    }
}
