package sslengine;

import peer.PeerInit;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class SSLPeer implements PeerInit {
    private final String serviceAccessPoint; // remoteObjectName since we will be using RMI
    private final InetSocketAddress address;
    private final SSLContext context;
    private final ServerRunnable serverRunnable;

    SSLPeer(String serviceAccessPoint, String hostAddress, int port) throws Exception {
        this.serviceAccessPoint = serviceAccessPoint;
        this.address = new InetSocketAddress(hostAddress, port);
        this.context = SSLEngineComms.createContext();

        System.out.println(this.address.getHostString() + this.address.getPort());

        this.serverRunnable = new ServerRunnable(this.context, hostAddress, port);
        Thread server = new Thread(serverRunnable);
        server.start();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java SSLPeer <serviceAccessPoint> <host> <port>");
            return;
        }

        // Create Peer
        SSLPeer peer;
        try {
            peer = new SSLPeer(args[0], args[1], Integer.parseInt(args[2]));
        } catch (Exception e) {
            //System.err.println("Error creating Peer");
            System.err.println(e.getMessage());
            return;
        }

        // Start RMI
        try {
            PeerInit stub = (PeerInit) UnicastRemoteObject.exportObject(peer, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(peer.serviceAccessPoint, stub);
        } catch (RemoteException e) {
            //System.err.println("Error starting RMI");
            System.err.println(e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(peer.serverRunnable::stop));
    }

    @Override
    public void backup(String filepath, int replicationDegree) throws RemoteException {

    }

    @Override
    public void restore(String filepath) throws RemoteException {

    }

    @Override
    public void delete(String filepath) throws RemoteException {

    }

    @Override
    public void reclaim(double diskspace) throws RemoteException {

    }

    @Override
    public String state() throws RemoteException {
        return null;
    }

    @Override
    public void send(String host, int port, String message) {
        try {
            SSLEngineClient client = new SSLEngineClient(this.context, host, port);
            client.connect();
            client.write(message);
            client.read();
            client.shutdown();
        } catch (Exception e) {
            System.out.println("[CLIENT] Can't send message");
        }
    }
}
