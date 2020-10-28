# Filet
Kotlin library for efficient networking based on coroutines.

## Principles

### Protocol
Sending multiples messages of variable size over a single connection is challenging.
For example : sending a large file "clogs the pipe", effectively preventing us
from sending anything while the transfer isn't finished.
The solution here is to wrap parts into small packets that won't block the stream for too long.

#### Packet
Unit of transmission, sent atomically in one piece.
Its size is inferior to MAX_PACKET_SIZE, a value set based on the speed of the underlying connection.
The smaller this value, the more reactive the transmission. However, this will also affect negatively the
overall efficiency when there are not so many transmissions happening at the same time (less buffering).
The overhead of a single packet is currently 9 bytes (transmissionId: 4, packetId: 1, length: 4).
The packet also has a priority, so we can decide whether to send it asap or after other packets.

##### Serialization
Bring your own POJO. Define a custom (de)serializer for it or use a common serialization format.
Currently, there are integrations for jackson-databind through the `ser-jackson` artifact.

#### Transmission
The new problem here is that we receive pieces of data in disorder.
A transmission is holding those packets together through a common transmission ID sent with each packet.

### Transport
Filet itself is built over an abstraction layer for the underlying transport.
Currently, two implementations for a TCP transport are available. `marais.filet.transport.impl.TcpTransport` directly
uses Java NIO `AsynchronousSocketChannel`. The other implementation is based on [Ktor](https://ktor.io) raw sockets
and is provided through the `transport-ktor` artifact.

## Implementation details
```
Client -> [Modules (Objects)] -> Serializer -> [Modules (Bytes)] -> Transport
```

### Threading model
This library is tightly coupled with Kotlin coroutines. Even though its considered bad practice,
this library will spawn coroutines by itself (e.g. when calling the `start` methods on Client and Server).
Those coroutines are scheduled to run on Default and IO thread pools.

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
    implementation("com.github.Gui-Yom.filet:filet:0.5.0")
    // Additional module to use the ktor sockets transport implementation
    implementation("com.github.Gui-Yom.filet:transport-ktor:0.5.0")
    // Additional module to use jackson databind as the serialization provider
    implementation("com.github.Gui-Yom.filet:ser-jackson:0.5.0")
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
 - Unified address system
 - Allow a server to listen on multiple transports
 - Annotation processing to map objects to packet identifiers at compilation
 - provide an UDT transport impl
 - Find something for the jungle of type casts in the serialization system that is making me want to kill myself
 - Implement a buffer swap, or a buffer pool because the repeated buffer allocations makes me want to die even harder
 - Moar documentation

## 1.0.0 ?
When the api is stable enough
