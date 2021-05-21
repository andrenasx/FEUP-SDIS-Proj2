package chord;

import messages.*;
import sslengine.SSLEngineClient;
import sslengine.SSLEngineComms;
import sslengine.SSLEnginePeer;
import utils.Utils;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static utils.Utils.generateId;

public class ChordNode extends SSLEnginePeer {
    private boolean boot;
    private ChordNodeReference self;
    private ChordNodeReference bootPeer;
    private ChordNodeReference predecessor;
    private ChordNodeReference[] routingTable = new ChordNodeReference[Utils.CHORD_M];
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(3);
    private int next = 1;

    public ChordNode(InetSocketAddress socketAddress, InetSocketAddress bootSocketAddress, boolean boot) throws Exception {
        super(SSLEngineComms.createContext(), socketAddress);
        this.boot = boot;

        this.self = new ChordNodeReference(this.getSocketAddress(), -1);
        this.bootPeer = new ChordNodeReference(bootSocketAddress, -1);

        System.out.println("My Address " + this.getSocketAddress());
        System.out.println("Boot Address " + bootPeer.getSocketAddress());
    }

    protected void startPeriodicStabilize() {
        scheduler.scheduleAtFixedRate(this::stabilize, 10, 10, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::fixFingers,10, 5, TimeUnit.SECONDS);
        //scheduler.scheduleAtFixedRate(this::checkPredecessor,10, 14, TimeUnit.SECONDS);
    }

    public synchronized ChordNodeReference getPredecessor() {
        return predecessor;
    }

    public synchronized void setPredecessor(ChordNodeReference predecessor) {
        this.predecessor = predecessor;
    }

    public synchronized ChordNodeReference getSuccessor() {
        return routingTable[0];
    }

    public synchronized void setSuccessor(ChordNodeReference successor) {
        this.routingTable[0] = successor;
    }

    public synchronized ChordNodeReference getRoutingTable(int position) {
        return routingTable[position - 1];
    }

    public synchronized void setChordNodeReference(int position, ChordNodeReference reference) {
        this.routingTable[position-1] = reference;
    }

    public synchronized ChordNodeReference getChordNodeReference(int position) {
        return this.routingTable[position-1];
    }

    public void join() {
        InetSocketAddress bootSocketAddress = bootPeer.getSocketAddress();

        if (this.boot) {
            this.self.setGuid(generateId(bootSocketAddress));

            //boot peer will be its successor
            this.setSuccessor(new ChordNodeReference(bootSocketAddress, this.self.getGuid()));
            System.out.println("Peer started as boot with id: " + this.self.getGuid());
            return;
        }

        try {
            JoinMessage request = new JoinMessage(this.self);

            GuidMessage response = (GuidMessage) ChordMessage.create(this.sendAndReceiveMessage(bootSocketAddress, request.encode()));

            this.self.setGuid(response.getNewGuid());
            System.out.println("Peer with id " + response.getNewGuid());

            this.setSuccessor(response.getSuccessorReference());
        } catch (Exception e) {
            System.out.println("Could not exchange messages");
            e.printStackTrace();
        }
    }

    public ChordNodeReference findSuccessor(int guid) {
        System.out.println("\n\nfinding successor...");
        System.out.println(getSuccessor());

        if (this.getSuccessor().getGuid() == self.getGuid()) { //in case there's only one peer in the network
            System.out.println("returned self");
            return self;
        }

        if (between(guid, self.getGuid(), this.getSuccessor().getGuid(), true)) {
            System.out.println("returned successor");
            return this.getSuccessor();
        }

        ChordNodeReference closest = closestPrecedingNode(guid);
        System.out.println("Closest to " + guid + ": " + closest);

        try {
            LookupMessage request = new LookupMessage(self, guid);

            LookupReplyMessage response = (LookupReplyMessage) ChordMessage.create(this.sendAndReceiveMessage(closest.getSocketAddress(), request.encode()));

            return response.getSuccessor();
        } catch (Exception e) {
            System.out.println("Could not exchange messages");
            e.printStackTrace();
            return self;
        }
    }

    public void stabilize() {
        System.out.println("\n\nstabilizing...");

        try {
            PredecessorMessage request = new PredecessorMessage(self);

            PredecessorReplyMessage response = (PredecessorReplyMessage) ChordMessage.create(this.sendAndReceiveMessage(getSuccessor().getSocketAddress(), request.encode()));

            ChordNodeReference predecessor = response.getPredecessor();
            if (predecessor != null && between(predecessor.getGuid(), self.getGuid(), getSuccessor().getGuid(), false)) {
                //set node successor
                this.setSuccessor(predecessor);
            }

            if (this.getSuccessor().getGuid() != this.self.getGuid())
                notify(getSuccessor());

        } catch (Exception e) {
            System.out.println("Could not exchange messages");
            e.printStackTrace();
        }
    }

    public void notify(ChordNodeReference successor) {
        try {
            NotifyMessage request = new NotifyMessage(self);

            this.sendMessage(successor.getSocketAddress(), request.encode());
        } catch (Exception e) {
            System.out.println("Could not send notify to Peer");
            e.printStackTrace();
        }
    }

    public void fixFingers() {
        System.out.println("\n\nfixing fingers...");

        int guid = this.self.getGuid() + (int) Math.pow(2, this.next - 1);
        guid = guid % Utils.CHORD_MAX_PEERS;

        this.setChordNodeReference(this.next, findSuccessor(guid));

        this.next++;
        if (this.next > Utils.CHORD_M) {
            this.next = 1;
        }
    }

    public void checkPredecessor() {
        if (this.predecessor != null) {
            System.out.println("\n\nchecking predecessor...");

            if (!this.connectToPeer(this.predecessor.getSocketAddress())) {
                System.err.println("Couldn't connect to predecessor");
                this.predecessor = null;
            }
        }
    }

    public ChordNodeReference closestPrecedingNode(int id) {
        for (int i = Utils.CHORD_M ; i >= 1; i--) {
            ChordNodeReference precedingNode = getChordNodeReference(i);
            if (precedingNode != null && between(precedingNode.getGuid(), self.getGuid(), id, false))
                return precedingNode;
        }
        return self;
    }

    public boolean between(int id, int currentId, int successorId, boolean includeSuccessor) {
        if (includeSuccessor)
            return currentId < successorId ? (currentId < id && id <= successorId) : (successorId >= id || id > currentId);
        else
            return currentId < successorId ? (currentId < id && id < successorId) : (successorId > id || id > currentId);
    }

    public ChordNodeReference getSelfReference() {
        return this.self;
    }

    public ChordNodeReference getBootReference() {
        return this.bootPeer;
    }

    public String chordState() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Node id: ").append(this.self.getGuid()).append("\n");
        stringBuilder.append("\nPredecessor: ").append(this.predecessor).append("\n");
        stringBuilder.append("\nRouting table:\n").append("\n");
        for (int i = 0; i < routingTable.length; i++) {
            stringBuilder.append(i).append("-").append(routingTable[i]).append("\n");
        }

        return stringBuilder.toString();
    }
}
