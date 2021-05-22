package sslengine;

import chord.ChordNode;
import messages.ChordMessage;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * An SSL/TLS server, that will listen to a specific address and port and serve SSL/TLS connections
 * compatible with the protocol it applies.
 * <p/>
 * After initialization {@link SSLEngineServer#start()} should be called so the server starts to listen to
 * new connection requests. At this point, start is blocking, so, in order to be able to gracefully stop
 * the server, a {@link Runnable} containing a server object should be created. This runnable should
 * start the server in its run method and also provide a stop method, which will call {@link SSLEngineServer#stop()}.
 * </p>
 * NioSslServer makes use of Java NIO, and specifically listens to new connection requests with a {@link ServerSocketChannel}, which will
 * create new {@link SocketChannel}s and a {@link Selector} which serves all the connections in one thread.
 */
public class SSLEngineServer extends SSLEngineComms {
	
	/**
	 * Declares if the server is active to serve and create new connections.
	 */
	private boolean active;
	
    /**
     * The context will be initialized with a specific SSL/TLS protocol and will then be used
     * to create {@link SSLEngine} classes for each new connection that arrives to the server.
     */
    private final SSLContext context;

    /**
     * A part of Java NIO that will be used to serve all connections to the server in one thread.
     */
    private final Selector selector;

    private final SocketAddress socketAddress;

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(20);

    /**
     * Server is designed to apply an SSL/TLS protocol and listen to an IP address and port.
     *
     * @param socketAddress - the address this server will listen to.
     * @throws Exception
     */
    public SSLEngineServer(SSLContext context, InetSocketAddress socketAddress) throws Exception {
        this.context = context;

        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(socketAddress);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        this.socketAddress = serverSocketChannel.socket().getLocalSocketAddress();
        active = true;
    }

    /**
     * Should be called in order the server to start listening to new connections.
     * This method will run in a loop as long as the server is active. In order to stop the server
     * you should use {@link SSLEngineServer#stop()} which will set it to inactive state
     * and also wake up the listener, which may be in blocking select() state.
     *
     * @throws Exception
     */
    public void start() {
        System.out.println("Initialized and waiting for new connections...");

        try {
            while (isActive()) {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        SSLEngine engine = (SSLEngine) key.attachment();
                        //System.out.println("Server about to read data");

                        byte[] data = null;
                        try {
                            // read data and create ChordMessage
                            data = read(channel, engine);

                        } catch (Exception e){
                            System.out.println("Error reading message...");
                            e.printStackTrace();
                        }

                        // data is null if error or end of connection
                        if (data != null) {
                            try {
                                ChordMessage request = ChordMessage.create(data);
                                System.out.println("Server received: " + request);
                                this.scheduler.submit(request.getTask(((ChordNode) this), channel, engine));
                            } catch (Exception e) {
                                System.out.println("Couldn't parse request message");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        //System.out.println("Goodbye from Server!");
    }

    /**
     * Sets the server to an inactive state, in order to exit the reading loop in {@link SSLEngineServer#start()}
     * and also wakes up the selector, which may be in select() blocking state.
     */
    public void stop() {
        System.out.println("Will now close server...");
    	active = false;
    	executor.shutdown();
    	scheduler.shutdown();
    	selector.wakeup();
    }

    /**
     * Will be called after a new connection request arrives to the server. Creates the {@link SocketChannel} that will
     * be used as the network layer link, and the {@link SSLEngine} that will encrypt and decrypt all the data
     * that will be exchanged during the session with this specific client.
     *
     * @param key - the key dedicated to the {@link ServerSocketChannel} used by the server to listen to new connection requests.
     * @throws Exception
     */
    private void accept(SelectionKey key) throws Exception {
        //System.out.println("New connection request!");

        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);

        setByteBuffers(engine.getSession());

        engine.beginHandshake();

        if (doHandshake(socketChannel, engine)) {
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } else {
            socketChannel.close();
            System.out.println("Connection closed due to handshake failure.");
        }
    }

    public void write(SocketChannel socketChannel, SSLEngine engine, byte[] message) throws IOException {
        super.write(socketChannel, engine, message);
    }

    /**
     * Determines if the the server is active or not.
     *
     * @return if the server is active or not.
     */
    private boolean isActive() {
        return active;
    }

    public InetSocketAddress getSocketAddress() {
        return (InetSocketAddress) this.socketAddress;
    }
}
