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

### API
```kotlin
client = Client(TcpTransport.Client())
client.transmit {
val packet = MessagePacket("")
it.sendPacket(packet)

val fileTransmission = FileTransmission(it)
fileTransmission.begin()
}
client.handle {

}
```
