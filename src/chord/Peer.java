package chord;

import channel.Server;
import tasks.chord.Task;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

public class Peer extends ChordNode {
    private final String serviceAccessPoint;
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(20);

    public Peer(InetSocketAddress socketAddress, boolean boot, String serviceAccessPoint) {
        super(socketAddress,boot);
        this.serviceAccessPoint = serviceAccessPoint;

        this.startPeer();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
                System.out.println("Usage: java Peer <serviceAccessPoint> <PeerAddress> <PeerPort> [-b]");
            return;
        }

        String serviceAccessPoint = args[0];

        InetAddress address = InetAddress.getByName(args[1]);
        int port = Integer.parseInt(args[2]);
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        boolean boot = (args.length > 3 && args[3].equals("-b"));

        //Creating peer
        try {
            new Peer(socketAddress, boot, serviceAccessPoint);
        } catch (Exception e) {
            System.err.println("Error creating Peer");
        }
    }

    public void startPeer(){
        //start server after joining
        this.start();

        //handles join
        this.joinRing();

    }

    public void submitThread(Task task){
        this.scheduler.submit(task);
    }

}
