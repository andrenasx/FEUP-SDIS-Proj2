package sslsocket;

import messages.Message;
import peer.Peer;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class SSLSocketPeer implements Runnable {
    private final SSLServerSocket serverSocket;
    protected InetSocketAddress socketAddress;
    private boolean active;
    private final ScheduledExecutorService sslScheduler = new ScheduledThreadPoolExecutor(16);

    public SSLSocketPeer(InetSocketAddress socketAddress) throws IOException {
        this.socketAddress = socketAddress;
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        // Truststore
        System.setProperty("javax.net.ssl.trustStore", "../keys/truststore");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        // Keystore
        System.setProperty("javax.net.ssl.keyStore", "../keys/server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = (SSLServerSocket) factory.createServerSocket(socketAddress.getPort());
        serverSocket.setEnabledCipherSuites(factory.getDefaultCipherSuites());
        this.active = true;
        new Thread(this).start();
    }

    // SERVER SIDE
    @Override
    public void run() {
        while (this.active) {
            SSLSocket socket;
            try {
                socket = (SSLSocket) this.serverSocket.accept();

                // Create message received
                Message message = (Message) new ObjectInputStream(socket.getInputStream()).readObject();

                // Submit task
                this.sslScheduler.execute(message.getTask((Peer) this, socket));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() throws IOException {
        this.active = false;
        this.serverSocket.close();
        System.out.println("[SSLSocket] Server close successfully");
    }

    // CLIENT SIDE
    public SSLSocket createClient(InetSocketAddress serverSocketAddress, int timeout) throws IOException {
        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket clientSocket = (SSLSocket) socketFactory.createSocket(serverSocketAddress.getAddress().getHostAddress(), serverSocketAddress.getPort());
        clientSocket.startHandshake();
        clientSocket.setSoTimeout(timeout);
        return clientSocket;
    }

    public void closeClient(SSLSocket clientSocket) throws IOException {
        clientSocket.close();
    }

    public void sendMessage(SSLSocket clientSocket, Message message) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());
        os.writeObject(message);
        os.flush();
    }

    public Message readMessage(SSLSocket clientSocket) throws Exception {
        return (Message) new ObjectInputStream(clientSocket.getInputStream()).readObject();
    }

    public Message sendAndReceiveMessage(InetSocketAddress serverSocketAddress, Message toSend) throws Exception {
        return this.sendAndReceiveMessage(serverSocketAddress, toSend, 0);
    }

    public Message sendAndReceiveMessage(InetSocketAddress serverSocketAddress, Message toSend, int timeout) throws Exception {
        SSLSocket clientSocket = this.createClient(serverSocketAddress, timeout);
        this.sendMessage(clientSocket, toSend);
        Message message;
        try {
            message = this.readMessage(clientSocket);
        } catch (Exception e) {
            clientSocket.close();
            return null;
        }

        clientSocket.close();

        return message;
    }

    public void sendClientMessage(InetSocketAddress serverSocketAddress, Message toSend) throws Exception {
        sendClientMessage(serverSocketAddress, toSend, 0);
    }

    public void sendClientMessage(InetSocketAddress serverSocketAddress, Message toSend, int timeout) throws IOException {
        SSLSocket clientSocket = this.createClient(serverSocketAddress, timeout);
        this.sendMessage(clientSocket, toSend);
    }
}
