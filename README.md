# Filet
Kotlin library for efficient networking based on NIO Channels and coroutines.

## Principles

### Base protocol

#### Packet
Unit of transmission with size <= MAX_PACKET_SIZE <= Integer.MAX_SIZE.
Use a packet id representing the type of the packet (byte).

#### Transmission
A relatively coherent piece of data (text message, file).
Made up of one or more packets.
Use a transmission id (byte).
Fragmenting a transmission in packets allows for stream multiplexing
(sending a message while also transmitting a big file).

#### Transport
The underlying protocol powering the data transfers.
Currently, two implementations of a TCP transport are available. `marais.filet.transport.impl.TcpTransport` directly
uses `java.nio.AsynchronousSocketChannel`. The other implementation is based on [Ktor](https://ktor.io) raw sockets
and is served through the `filet-ktor` artifact.

### API (Kotlin)
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

        override fun getPacketClass(): Class<DummyPacket> = DummyPacket::class.java

        override fun writeData(obj: DummyPacket, buffer: ByteBuffer): Int {
            buffer.putInt(obj.a)
            return 4
        }
    }
}
```

### Inner workings

```
Client -> Queue (Objects) -> [Module 0, Module 1, ...] -> Queue (Buffers) -> Transport
```
