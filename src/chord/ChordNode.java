package chord;

import messages.*;
import sslengine.SSLEngineComms;
import sslengine.SSLEnginePeer;
import utils.Utils;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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

        System.out.println("[CHORD NODE] My Address " + this.getSocketAddress());
        System.out.println("[CHORD NODE] Boot Address " + bootPeer.getSocketAddress());
    }

    protected void startPeriodicStabilize() {
        /*scheduler.scheduleAtFixedRate(this::stabilize, 5, 10, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::fixFingers,3, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkPredecessor,10, 15, TimeUnit.SECONDS);*/
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

    public boolean join() {
        InetSocketAddress bootSocketAddress = bootPeer.getSocketAddress();

        if (this.boot) {
            this.self.setGuid(generateId(bootSocketAddress));

            //boot peer will be its successor
            this.setSuccessor(new ChordNodeReference(bootSocketAddress, this.self.getGuid()));
            System.out.println("[CHORD NODE] Boot with guid: " + this.self.getGuid());
            return true;
        }

        try {
            JoinMessage request = new JoinMessage(this.self);

            System.out.println("Client wrote: " + request);
            GuidMessage response = (GuidMessage) ChordMessage.create(this.sendAndReceiveMessage(bootSocketAddress, request.encode(), 100));
            System.out.println("Client received: " + response);

            this.self.setGuid(response.getNewGuid());
            System.out.println("[CHORD NODE] Guid " + response.getNewGuid());

            this.setSuccessor(response.getSuccessorReference());
            return true;
        } catch (Exception e) {
            System.out.println("Could not join ring");
            e.printStackTrace();
            return false;
        }
    }

    public ChordNodeReference findSuccessor(int guid) {
        System.out.println("\n\n[CHORD-PERIODIC] finding successor...");
        System.out.println(getSuccessor());

        if (this.getSuccessor().getGuid() == self.getGuid()) { //in case there's only one peer in the network
            System.out.println("returned self");
            return self;
        }

        if (this.between(guid, self.getGuid(), this.getSuccessor().getGuid(), true)) {
            System.out.println("returned successor");
            return this.getSuccessor();
        }

        // Return node if we have an entry for it TODO
        for (int i = Utils.CHORD_M ; i >= 1; i--) {
            ChordNodeReference node = this.getChordNodeReference(i);
            if (node != null && node.getGuid() == guid)
                return node;
        }

        ChordNodeReference closest = this.closestPrecedingNode(guid);
        System.out.println("Closest to " + guid + ": " + closest);

        try {
            LookupMessage request = new LookupMessage(self, guid);

            System.out.println("Client wrote: " + request);
            LookupReplyMessage response = (LookupReplyMessage) ChordMessage.create(this.sendAndReceiveMessage(closest.getSocketAddress(), request.encode(), 200));
            System.out.println("Client received: " + response);

            return response.getSuccessor();
        } catch (Exception e) {
            System.out.println("Could not exchange messages");
            e.printStackTrace();
            return self;
        }
    }

    public void stabilize() {
        System.out.println("\n\n[CHORD-PERIODIC] stabilizing...");

        try {
            PredecessorMessage request = new PredecessorMessage(self);

            System.out.println("Client wrote: " + request);
            PredecessorReplyMessage response = (PredecessorReplyMessage) ChordMessage.create(this.sendAndReceiveMessage(getSuccessor().getSocketAddress(), request.encode(), 100));
            System.out.println("Client received: " + response);

            ChordNodeReference predecessor = response.getPredecessor();
            if (predecessor != null && this.between(predecessor.getGuid(), self.getGuid(), getSuccessor().getGuid(), false)) {
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

            System.out.println("NOTIFYING " + successor.getGuid());
            System.out.println("Client wrote: " + request);
            this.sendMessage(successor.getSocketAddress(), request.encode());
        } catch (Exception e) {
            System.out.println("Could not send notify to Peer");
            e.printStackTrace();
        }
    }

    public void fixFingers() {
        System.out.println("\n\n[CHORD-PERIODIC] fixing fingers...");

        int guid = this.self.getGuid() + (int) Math.pow(2, this.next - 1);
        guid = guid % Utils.CHORD_MAX_PEERS;

        this.setChordNodeReference(this.next, this.findSuccessor(guid));

        this.next++;
        if (this.next > Utils.CHORD_M) {
            this.next = 1;
        }
    }

    public void checkPredecessor() {
        if (this.predecessor != null) {
            System.out.println("\n\n[CHORD-PERIODIC] checking predecessor...");

            if (!this.connectToPeer(this.predecessor.getSocketAddress())) {
                System.err.println("Couldn't connect to predecessor");
                this.predecessor = null;
            }
        }
    }

    public ChordNodeReference closestPrecedingNode(int id) {
        for (int i = Utils.CHORD_M ; i >= 1; i--) {
            ChordNodeReference precedingNode = this.getChordNodeReference(i);
            if (precedingNode != null && this.between(precedingNode.getGuid(), self.getGuid(), id, false))
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
        stringBuilder.append("\nRouting table:\n");
        for (int i = 0; i < routingTable.length; i++) {
            stringBuilder.append(i).append("-").append(routingTable[i]).append("\n");
        }

        return stringBuilder.toString();
    }
}
