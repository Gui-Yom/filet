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

### API (Kotlin)
```kotlin
val client = Client(CoroutineScope(), Pipeline(DummyModule))
client.onReceive {
    when (it) {
        is DummyPacket -> {
            it.a
        }
    }
}
client.registerType(DummyPacket)
client.start(DummyTransport.Client)
client.transmit {
    sendPacket(DummyPacket())
}

class DummyPacket(val a: Int = 0) {

    companion object : AbstractPacketSerializer<DummyPacket>(0) {
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
