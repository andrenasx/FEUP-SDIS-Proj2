package sslengine;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
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

    private boolean blocking;


    /**
     * Initiates the engine to run as a client using peer information, and allocates space for the
     * buffers that will be used by the engine.
     *
     * @param socketAddress The socket address of the server.
     * @throws Exception
     */
    public SSLEngineClient(SSLContext context, InetSocketAddress socketAddress, boolean blocking) throws Exception  {
    	this.socketAddress = socketAddress;
    	this.blocking = blocking;

        this.engine = context.createSSLEngine(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
        this.engine.setUseClientMode(true);

        setByteBuffers(engine.getSession());
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
    	boolean handshaked = doHandshake(socketChannel, engine);
    	socketChannel.configureBlocking(blocking);
    	return handshaked;
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

        return read(socketChannel, engine);
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

    public byte[] receive(int timeToRead) throws MessageTimeoutException, IOException {
        byte[] reply;
        int attempt = 0;
        while ((reply = this.read(socketChannel, engine)) == null && attempt < 50) {
            attempt++;
            try {
                Thread.sleep(timeToRead * 3L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (reply == null) {
            throw new MessageTimeoutException("Message took too long to receive!");
        }

        return reply;
    }

}
