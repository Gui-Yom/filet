package marais.filet

import marais.filet.transport.ServerTransport

/**
 * The server listen for connections from clients.
 */
class Server(val transports: List<ServerTransport>)