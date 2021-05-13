package channel;

import chord.Peer;
import messages.chord.ChordMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Server extends Thread {

    private SSLServerSocket server_socket;
    private SSLSocket socket;
    protected InetSocketAddress address;

    public Server(InetSocketAddress socketAddress, boolean boot) {
        //starts server
        System.setProperty("javax.net.ssl.trustStore", "./keys/truststore");
        System.setProperty("javax.net.ssl.trustStoreType","JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        System.setProperty("javax.net.ssl.keyStore", "./keys/server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        SSLServerSocketFactory ssl_server_socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        int port = socketAddress.getPort();
        InetAddress address = socketAddress.getAddress();

        try {
            //if it is not boot peer creates server in the next available port
            if(boot) {
                server_socket = (SSLServerSocket) ssl_server_socket_factory.createServerSocket(port, 0, address);
            }
            else{
                server_socket = (SSLServerSocket) ssl_server_socket_factory.createServerSocket(0);
            }

            this.address = (InetSocketAddress) server_socket.getLocalSocketAddress();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error opening port " + port);
        }

        System.out.println("server ready!");
    }

    public SSLSocket getClientSocket(){
        return socket;
    }

    @Override
    public void run() {
        while(true) {
            try {
                this.receive_message();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void receive_message() throws IOException{
        socket = (SSLSocket) server_socket.accept();
        handle_connection(socket).submitTask((Peer) this);
    }

    public ChordMessage receive_message(SSLSocket socket) throws IOException{
        return handle_connection(socket);
    }

    private ChordMessage handle_connection(SSLSocket socket) {
        DataInputStream input = null;
        byte[] toRead = null;
        try {
            input = new DataInputStream(socket.getInputStream());
            int length = input.readInt();                    // read length of incoming message
            if(length>0) {
                toRead = new byte[length];
                input.readFully(toRead, 0, toRead.length); // read the message
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ChordMessage message = null;
        try {
            message = ChordMessage.decode(toRead);
            System.out.println("RECEIVED: " + message);
        } catch (Exception e) {
            System.err.println("Error while reading message!");
            e.printStackTrace();
        }

        return message;
    }

    //----------CLIENT MODE FUNCTIONS---------

    public void send_message(ChordMessage msg, SSLSocket socket) {
        try {
            byte[] toSend = msg.encode();
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            dOut.writeInt(toSend.length);
            dOut.write(toSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //send message to server with no wait for response
    public SSLSocket send_message(ChordMessage msg, InetSocketAddress server) {
        SSLSocket socket = connect(server);
        if (socket == null) {
            return null;
        }
        try {
            byte[] toSend = msg.encode();
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            dOut.writeInt(toSend.length);
            dOut.write(toSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    public SSLSocket connect(InetSocketAddress server) {
        SSLSocketFactory ssl_socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = null;

        try {
            socket = (SSLSocket) ssl_socket_factory.createSocket(server.getAddress(), server.getPort());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't connect to peer!");
            return null;
        }

        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

        return socket;
    }

}