package chord;

import peer.OldPeer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

import static utils.Utils.generateId;

public class Peer{
    private int id;
    private boolean boot;
    private final String serviceAccessPoint;
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(5);
    private PeerStorage storage;
    private ReferenceTable table;


    public Peer(String[] args) throws IOException {
        InetAddress address = InetAddress.getByName(args[1]);
        int port = Integer.parseInt(args[2]);

        this.serviceAccessPoint = args[0];
        this.boot = (args.length > 3 && args[3].equals("-b"));

        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        // Create Peer Internal State
        //this.storage = PeerStorage.loadState(this);

        // Create chord reference table
        join(socketAddress);
    }

    public static void main(String[] args) {
        if (args.length < 3) {
                System.out.println("Usage: java Peer <serviceAccessPoint> <PeerAddress> <PeerPort> [-b]");
            return;
        }

        //Creating peer
        Peer peer;
        try {
            peer = new Peer(args);
        } catch (IOException e) {
            System.err.println("Error creating Peer");
        }
    }

    public void join(InetSocketAddress socketAddress){
        if(this.boot){
            this.id = generateId(socketAddress);

            table = new ReferenceTable(this.id, socketAddress);
            table.setSuccessor(this.id, socketAddress);

            System.out.println("Peer started as boot with id: " + this.id);
            return;
        }

        try{
            //connects to boot peer



        }catch(){

        }

    }

}
