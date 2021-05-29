package sslengine;

import utils.Utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class SSLEnginePeer extends SSLEngineServer {
    private SSLContext context;

    public SSLEnginePeer(SSLContext context, InetSocketAddress socketAddress) throws Exception {
        super(context, socketAddress);
        this.context = context;
    }

    public boolean connectToPeer(InetSocketAddress socketAddress) {
        SSLEngineClient client = new SSLEngineClient(this.context, socketAddress);
        boolean connected = false;

        try {
            connected = client.connect();
            client.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return connected;
    }

    public byte[] sendAndReceiveMessage(InetSocketAddress socketAddress, byte[] message, int delay) throws Exception {
        SSLEngineClient client = new SSLEngineClient(this.context, socketAddress);
        client.connect();
        client.write(message);

        byte[] response = client.read(delay);
        client.shutdown();

        return response;
    }

    public void sendMessage(InetSocketAddress socketAddress, byte[] message) throws Exception {
        SSLEngineClient client = new SSLEngineClient(this.context, socketAddress);
        client.connect();
        client.write(message);
        // client.shutdown(); Shutdown is done at the Server
    }

    public SSLContext getContext() {
        return context;
    }
}
