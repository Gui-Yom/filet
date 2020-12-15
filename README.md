# Filet

Kotlin library for efficient networking based on coroutines. (JVM only)

## Principles

Sending multiples messages of variable size over a single connection is challenging. For example : sending a large
file "clogs the pipe", effectively preventing us from sending anything while the transfer isn't finished. The solution
here is to wrap parts into small packets that won't block the stream for too long.

### Packet

Unit of transmission, sent atomically in one piece. Its size is inferior to MAX_PACKET_SIZE, a value set based on the
speed of the underlying connection. The smaller this value, the more reactive the transmission. However, this will also
affect negatively the overall efficiency when there are not so many transmissions happening at the same time (less
buffering). The overhead of a single packet is currently (transmissionId: 2, packetId: 2, length: 4) bytes.

#### Serialization

We need to know the objects we send and receive and how we send and receive them. For the first problem we need a
discriminator. The default encodes the fully qualified class name of the object in the packet header. For serialization,
define a custom (de)serializer for your class or use a common serialization format. There are currently integrations for
jackson-databind through the `ser-jackson` artifact.

### Transmission

A transmission is what binds packets together through a common transmission ID sent with each packet. It also has a
priority, used to sort packets in the send queue. The transmission system is totally optional, you can also use the
basic middleware for the normal-style behavior.

### Transport

Filet itself is built over an abstraction layer for the underlying transport. Currently, two implementations for a TCP
transport are available. `marais.filet.transport.impl.TcpTransport` directly uses Java NIO `AsynchronousSocketChannel`.
The other implementation is based on [Ktor](https://ktor.io) raw sockets and is provided through the `transport-ktor`
artifact.

## Implementation details

```
Client -> [Modules (Objects)] -> Serializer -> [Modules (Bytes)] -> Transport
```

### Threading model

This library is tightly coupled with Kotlin coroutines. Even though its considered bad practice, this library will spawn
coroutines by itself (e.g. when calling the `start` methods on Client and Server). Those coroutines are scheduled to run
on Default and IO thread pools. The coroutine scope is configurable.

## Notes (QUIC)

This transmission system is very similar in essence and execution to the QUIC protocol. The QUIC specification does
mention (briefly) a priority system, but the available QUIC libraries does not actually implement it.

## Installation

With Gradle :

```kotlin
repositories {
    // Artifacts are currently only published on Jitpack
    maven {
        setUrl("https://jitpack.io")
    }
}
dependencies {
    // Main module
    implementation("com.github.Gui-Yom.filet:filet:0.6.0")
    // Additional module to use the ktor sockets transport implementation
    implementation("com.github.Gui-Yom.filet:transport-ktor:0.6.0")
    // Additional module to use jackson databind as the serialization provider
    implementation("com.github.Gui-Yom.filet:ser-jackson:0.6.0")
}
```

## Example usage (Kotlin)

**Outdated**

```kotlin
val server = Server()
server.registerSerializer(DummyPacket)
server.connectionHandler {
    println("New connection !")
    true
}
server.handler { server, obj ->
    when (obj) {
        is DummyPacket -> {
            println("Received dummy packet : value=${obj.a}")
        }
    }
}
server.start(TcpTransport.Server(InetSocketAddress(InetAddress.getLoopbackAddress(), 4785)))

val client = Client()
client.registerSerializer(DummyPacket)
client.handler {
    when (it) {
        // Do something with it
    }
}
client.start(TcpTransport.Client(InetSocketAddress(InetAddress.getLoopbackAddress(), 4785)))
client.transmit {
    sendPacket(HandshakePacket())
}

class DummyPacket(val a: Int = 0) {

    companion object : PacketSerializer<DummyPacket>(0, 0) {
        override fun read(buffer: ByteBuffer): DummyPacket = DummyPacket(buffer.int)

        override fun getPacketKClass() = DummyPacket::class

        override fun writeData(obj: DummyPacket, buffer: ByteBuffer): Int {
            buffer.putInt(obj.a)
            return 4
        }
    }
}
```

## TODO

- Unified address system representing the underlying transport, maybe automated discovery through service loader
- Allow communication on multiple transports
- Annotation processor to create discriminators for objects at compile time
- UDT transport
- JeroMQ transport
- Find something for the jungle of type casts in the serialization system that is making me want to kill myself (clever
  use of inline functions)
- Implement a buffer swap, or a buffer pool because the repeated buffer allocations makes me want to die even harder
- Moar documentation

## 1.0.0 ?

When the api is stable enough
