package marais.filet.transport

object TcpTransport {
    class Client(val addr: InetAdd) : ClientTransport {

    }

    class Server : ServerTransport {

    }
}
