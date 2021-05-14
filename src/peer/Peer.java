package peer;

import chord.ChordNode;
import sslengine.SSLEngineComms;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Peer extends ChordNode implements PeerInit {
    private final String serviceAccessPoint;

    public Peer(String serviceAccessPoint, InetSocketAddress socketAddress, InetSocketAddress bootScoketAddress, boolean boot) throws Exception {
        super(socketAddress, bootScoketAddress, SSLEngineComms.createContext(), boot);
        this.serviceAccessPoint = serviceAccessPoint;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
                System.out.println("Usage: java Peer <serviceAccessPoint> <bootAddress> <bootPort> [-b]");
            return;
        }

        String serviceAccessPoint = args[0];

        /*InetAddress address = InetAddress.getByName(args[1]);
        int port = Integer.parseInt(args[2]);*/
        InetSocketAddress bootSocketAddress = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

        boolean boot = (args.length > 3 && args[3].equals("-b"));

        Peer peer;
        //Creating peer
        try {
            InetSocketAddress socketAddress;
            if (boot) {
                socketAddress = bootSocketAddress;
            }
            else {
                socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
            }

            peer = new Peer(serviceAccessPoint, socketAddress, bootSocketAddress, boot);
        } catch (Exception e) {
            System.err.println("Error creating Peer");
            return;
        }

        // Start RMI
        try {
            PeerInit stub = (PeerInit) UnicastRemoteObject.exportObject(peer, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(peer.serviceAccessPoint, stub);
        } catch (RemoteException e) {
            System.err.println("Error starting RMI");
        }
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
}
