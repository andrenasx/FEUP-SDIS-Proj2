package sslengine;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

public class SSLEngineClient extends SSLEngineComms {
    private final InetSocketAddress socketAddress;
    private final SSLEngine engine;
    private SocketChannel socketChannel;

    public SSLEngineClient(SSLContext context, InetSocketAddress socketAddress) {
        try {
            Thread.sleep(new Random().nextInt(500));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.socketAddress = socketAddress;

        this.engine = context.createSSLEngine(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
        this.engine.setUseClientMode(true);

        setByteBuffers(engine.getSession());
    }

    public boolean connect() throws Exception {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(this.socketAddress);
        while (!socketChannel.finishConnect()) {
            // can do something here...
        }

        engine.beginHandshake();
        boolean handshaked = doHandshake(socketChannel, engine);
        return handshaked;
    }

    public void write(byte[] message) throws IOException {
        write(socketChannel, engine, message);
    }

    public byte[] read(int delay) throws Exception {
        //System.out.println("Client about to read data");

        int attempt = 0;
        byte[] message = read(socketChannel, engine);
        while (message == null && attempt < 50) {
            message = read(socketChannel, engine);
            attempt++;
            Thread.sleep(delay);
        }

        return message;
    }

    public void shutdown() throws IOException {
        //System.out.println("About to close connection with the server...");
        closeConnection(socketChannel, engine);
        executor.shutdown();
        //System.out.println("Goodbye from Client!");
    }
}
