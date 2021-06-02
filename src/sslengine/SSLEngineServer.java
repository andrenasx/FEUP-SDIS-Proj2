package sslengine;

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

public class SSLEngineServer extends SSLEngineComms {
    private boolean active;
    private final SSLContext context;
    private final Selector selector;
    private final SocketAddress socketAddress;
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(20);

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

    public void start() {
        System.out.println("Initialized and waiting for new connections...");

        try {
            while (active) {
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
                    }
                    else if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        SSLEngine engine = (SSLEngine) key.attachment();
                        //System.out.println("Server about to read data");

                        byte[] data = null;
                        try {
                            // read data and create ChordMessage
                            data = read(channel, engine);

                        } catch (Exception e) {
                            System.out.println("Error reading message... " + e.getMessage());
                            //e.printStackTrace();
                        }

                        // data is null if error or end of connection
                        if (data != null) {
                            try {
                                /*Message request = Message.create(data);
                                //System.out.println("Server received: " + request);
                                if (request instanceof BackupMessage)
                                    key.cancel();
                                this.scheduler.submit(request.getTask(((ChordNode) this), channel, engine));*/
                            } catch (Exception e) {
                                System.out.println("Couldn't parse request message");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //System.out.println("Goodbye from Server!");
    }

    public void stop() {
        active = false;
        executor.shutdown();
        scheduler.shutdown();
        selector.wakeup();
        System.out.println("[SSLServer] Server shutdown successfully");
    }

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
        }
        else {
            socketChannel.close();
            System.out.println("Connection closed due to handshake failure.");
        }
    }

    public void write(SocketChannel socketChannel, SSLEngine engine, byte[] message) throws IOException {
        super.write(socketChannel, engine, message);
    }

    public void closeConnectionServer(SocketChannel socketChannel, SSLEngine engine) {
        try {
            super.closeConnection(socketChannel, engine);
        } catch (IOException e) {
            System.err.println("Couldn't close connection in Server");
            e.printStackTrace();
        }
    }

    public InetSocketAddress getSocketAddress() {
        return (InetSocketAddress) this.socketAddress;
    }
}
