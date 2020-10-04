# Filet
Kotlin library for efficient networking based on NIO Channels and coroutines.

## Principles

### Base protocol

#### Packet
Unit of transmission with size <= MAX_PACKET_SIZE <= Integer.MAX_SIZE.
Use a packet id representing the type of the packet (byte).

##### Serialization
There is no packet class, you can bring your own POJO and a serializer/deserializer to/from bytes for it.

#### Transmission
A relatively coherent piece of data (text message, file).
Made up of one or more packets.
Use a transmission id (byte).
Fragmenting a transmission in packets allows for stream multiplexing
(e.g. sending a message while also transmitting a big file).

#### Transport
The underlying protocol powering the data transfers.
Currently, two implementations of a TCP transport are available. `marais.filet.transport.impl.TcpTransport` directly
uses `java.nio.AsynchronousSocketChannel`. The other implementation is based on [Ktor](https://ktor.io) raw sockets
and is provided through the `filet-ktor` artifact.

### Implementation details
```
Client -> Queue (Objects) -> [Module 0, Module 1, ...] -> Queue (Buffers) -> Transport
```

#### Threading model
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
    implementation("com.github.Gui-Yom.filet:filet:0.2.0")
    // Additional module to use the ktor sockets transport implementation
    implementation("com.github.Gui-Yom.filet:filet-ktor:0.2.0")
}
```

## API (Kotlin)
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
 - Integration with kotlinx.serialization though a module
