package sslengine;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * An SSL/TLS client that connects to a server using its IP address and port.
 * <p/>
 * After initialization of a {@link SSLEngineClient} object, {@link SSLEngineClient#connect()} should be called,
 * in order to establish connection with the server.
 * <p/>
 * When the connection between the client and the object is established, {@link SSLEngineClient} provides
 * a public write and read method, in order to communicate with its peer. 
 *
 * @author <a href="mailto:alex.a.karnezis@gmail.com">Alex Karnezis</a>
 */
public class SSLEngineClient extends SSLEngineComms {

    /**
     * The socket of the server this client is configured to connect to.
     */
	private InetSocketAddress socketAddress;

	/**
	 * The engine that will be used to encrypt/decrypt data between this client and the server.
	 */
    private SSLEngine engine;

    /**
     * The socket channel that will be used as the transport link between this client and the server.
     */
    private SocketChannel socketChannel;


    /**
     * Initiates the engine to run as a client using peer information, and allocates space for the
     * buffers that will be used by the engine.
     *
     * @param socketAddress The socket address of the server.
     * @throws Exception
     */
    public SSLEngineClient(SSLContext context, InetSocketAddress socketAddress) throws Exception  {
    	this.socketAddress = socketAddress;

        engine = context.createSSLEngine(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
        engine.setUseClientMode(true);

        SSLSession session = engine.getSession();
        myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    /**
     * Opens a socket channel to communicate with the configured server and tries to complete the handshake protocol.
     *
     * @return True if client established a connection with the server, false otherwise.
     * @throws Exception
     */
    public boolean connect() throws Exception {
    	socketChannel = SocketChannel.open();
    	socketChannel.configureBlocking(false);
    	socketChannel.connect(this.socketAddress);
    	while (!socketChannel.finishConnect()) {
    		// can do something here...
    	}

    	engine.beginHandshake();
    	return doHandshake(socketChannel, engine);
    }

    /**
     * Public method to send a message to the server.
     *
     * @param message - message to be sent to the server.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    public void write(byte[] message) throws IOException {
        //System.out.println("Client about to write data");
        write(socketChannel, engine, message);
    }

    /**
     * Public method to try to read from the server.
     *
     * @throws Exception
     */
    public byte[] read() throws Exception {
        //System.out.println("Client about to read data");

        byte[] message = null;
        while (message == null) {
            message = read(socketChannel, engine);
        }
        return message;
    }

    /**
     * Should be called when the client wants to explicitly close the connection to the server.
     *
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    public void shutdown() throws IOException {
        //System.out.println("About to close connection with the server...");
        closeConnection(socketChannel, engine);
        executor.shutdown();
        //System.out.println("Goodbye from Client!");
    }

}
