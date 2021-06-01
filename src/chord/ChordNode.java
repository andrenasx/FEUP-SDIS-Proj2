package chord;

import messages.chord.*;
import messages.protocol.CopyKeysMessage;
import messages.protocol.CopyKeysReplyMessage;
import messages.protocol.DeleteMessage;
import messages.protocol.GetFileMessage;
import peer.Peer;
import peer.storage.NodeStorage;
import peer.storage.StorageFile;
import sslsocket.SSLSocketPeer;
import utils.Utils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static utils.Utils.generateId;

public class ChordNode extends SSLSocketPeer {
    private boolean boot;
    private ChordNodeReference self;
    private ChordNodeReference bootPeer;
    private ChordNodeReference predecessor;
    private ChordNodeReference[] routingTable = new ChordNodeReference[Utils.CHORD_M];
    private ChordNodeReference[] successorsList = new ChordNodeReference[3];
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(3);
    private NodeStorage nodeStorage;
    private int next = 1;

    public ChordNode(InetSocketAddress socketAddress, InetSocketAddress bootSocketAddress, boolean boot) throws Exception {
        super(socketAddress);
        this.boot = boot;

        this.self = new ChordNodeReference(socketAddress, -1);
        this.bootPeer = new ChordNodeReference(bootSocketAddress, -1);

        System.out.println("[CHORD] My Address " + socketAddress);
        System.out.println("[CHORD] Boot Address " + bootSocketAddress);
    }

    protected void startPeriodicStabilize() {
        System.out.println("[CHORD] Started periodic stabilization");
        this.scheduler.scheduleAtFixedRate(this::stabilize, 4, 5, TimeUnit.SECONDS);
        this.scheduler.scheduleAtFixedRate(this::fixFingers, 2, 3, TimeUnit.SECONDS);
        this.scheduler.scheduleAtFixedRate(this::checkPredecessor, 6, 10, TimeUnit.SECONDS);
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

    public synchronized ChordNodeReference[] getSuccessorsList() {
        return successorsList;
    }

    public synchronized void setSuccessorsList(int position, ChordNodeReference node) {
        this.successorsList[position] = node;
    }

    public synchronized void setChordNodeReference(int position, ChordNodeReference reference) {
        this.routingTable[position - 1] = reference;
    }

    public synchronized ChordNodeReference getChordNodeReference(int position) {
        return this.routingTable[position - 1];
    }

    public boolean join() {
        InetSocketAddress bootSocketAddress = this.bootPeer.getSocketAddress();

        if (this.boot) {
            this.self.setGuid(generateId(bootSocketAddress));

            //boot peer will be its successor
            this.setSuccessor(new ChordNodeReference(bootSocketAddress, this.self.getGuid()));
            this.setSuccessorsList(0, getSuccessor());

            System.out.println("[CHORD NODE] Boot with guid: " + this.self.getGuid());

            // Load storage
            this.nodeStorage = NodeStorage.loadState(this.self.getGuid());
            return true;
        }

        try {
            JoinMessage joinMessage = new JoinMessage(this.self);

            //System.out.println("Client wrote: " + request);
            GuidMessage guidMessage = (GuidMessage) this.sendAndReceiveMessage(bootSocketAddress, joinMessage);
            //System.out.println("Client received: " + response);

            this.self.setGuid(guidMessage.getNewGuid());
            System.out.println("[CHORD] Guid " + guidMessage.getNewGuid());

            this.setSuccessor(guidMessage.getSuccessorReference());
            this.setSuccessorsList(0, getSuccessor());
        } catch (Exception e) {
            System.out.println("[ERROR-CHORD] Could not join ring");
            e.printStackTrace();
            return false;
        }

        // Load storage
        this.nodeStorage = NodeStorage.loadState(this.self.getGuid());

        // Reclaim Keys (stored files that should be on this node)
        try {
            CopyKeysMessage copyKeysMessage = new CopyKeysMessage(this.self);
            CopyKeysReplyMessage copyKeysReplyMessage = (CopyKeysReplyMessage) this.sendAndReceiveMessage(this.getSuccessor().getSocketAddress(), copyKeysMessage);

            List<Future<Boolean>> files = new ArrayList<>();
            for (StorageFile delegatedFile : copyKeysReplyMessage.getDelegatedFiles()) {
                files.add(((Peer) this).getScheduler().submit(() -> getDelegatedFile(delegatedFile)));
            }

            for (Future<Boolean> result : files) {
                if (!result.get()) {
                    System.err.println("[ERROR-CHORD] Couldn't restore delegated file");
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR-CHORD] Could not get delegated files");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean getDelegatedFile(StorageFile delegatedFile) {
        GetFileMessage getFileMessage = new GetFileMessage(this.getSuccessor(), delegatedFile.getFileId());
        if (!((Peer) this).restoreFile(getFileMessage, this.getSuccessor(), this.getNodeStorage().getStoragePath() + delegatedFile.getFileId())) {
            System.err.println("[ERROR-CHORD] Couldn't restore " + delegatedFile.getFilePath());
            return false;
        }

        DeleteMessage deleteMessage = new DeleteMessage(this.getSuccessor(), delegatedFile.getFileId());
        String result = ((Peer) this).deleteFile(deleteMessage, this.getSuccessor(), delegatedFile, true);
        if (result.contains("ERROR")) return false;

        this.nodeStorage.addStoredFile(delegatedFile);
        return true;
    }

    public ChordNodeReference findSuccessor(int guid) {
        //System.out.println("\n\n[CHORD] Finding successor...");
        //System.out.println(getSuccessor());

        if (this.getSuccessor().getGuid() == self.getGuid()) { //in case there's only one peer in the network
            //System.out.println("returned self");
            return self;
        }

        if (this.between(guid, self.getGuid(), this.getSuccessor().getGuid(), true)) {
            //System.out.println("returned successor");
            return this.getSuccessor();
        }

        // Return node if we have an entry for it TODO
        /*for (int i = Utils.CHORD_M; i >= 1; i--) {
            ChordNodeReference node = this.getChordNodeReference(i);
            if (node != null && node.getGuid() == guid)
                return node;
        }*/

        ChordNodeReference closest = this.closestPrecedingNode(guid);
        //System.out.println("Closest to " + guid + ": " + closest);

        try {
            LookupMessage request = new LookupMessage(self, guid);

            //System.out.println("Client wrote: " + request);
            LookupReplyMessage response = (LookupReplyMessage) this.sendAndReceiveMessage(closest.getSocketAddress(), request);
            //System.out.println("Client received: " + response);

            return response.getNode();
        } catch (Exception e) {
            System.out.println("[ERROR-CHORD] Could not exchange messages");
            //e.printStackTrace();
            return self;
        }
    }

    public void stabilize() {
        //System.out.println("\n\n[CHORD-PERIODIC] stabilizing...");
        this.setSuccessorsList(0, getSuccessor());

        boolean success = false;
        int nextSuccessor = 0;
        while (!success) {
            try {
                SuccessorsMessage request = new SuccessorsMessage(self);

                //System.out.println("Client wrote: " + request);
                SuccessorsReplyMessage response = (SuccessorsReplyMessage) this.sendAndReceiveMessage(getSuccessor().getSocketAddress(), request);
                //System.out.println("Client received: " + response);
                System.arraycopy(response.getSuccessors(), 0, this.successorsList, 1, 2);
                //System.out.println("SUCCESSORS LIST: " + Arrays.toString(this.successorsList));
                success = true;

            } catch (Exception e) {
                System.out.println("[ERROR-CHORD] Successor down");
                nextSuccessor++;
                if (nextSuccessor == 3) return;
                setSuccessor(this.successorsList[nextSuccessor]);
                this.setSuccessorsList(0, getSuccessor());
                //e.printStackTrace();
            }
        }

        try {
            PredecessorMessage request = new PredecessorMessage(self);

            //System.out.println("Client wrote: " + request);
            PredecessorReplyMessage response = (PredecessorReplyMessage) this.sendAndReceiveMessage(getSuccessor().getSocketAddress(), request);
            //System.out.println("Client received: " + response);

            ChordNodeReference predecessor = response.getPredecessor();
            if (predecessor != null && this.between(predecessor.getGuid(), self.getGuid(), getSuccessor().getGuid(), false)) {
                this.setSuccessor(predecessor);
            }

            if (this.getSuccessor().getGuid() != this.self.getGuid())
                notify(getSuccessor());

        } catch (Exception e) {
            System.out.println("[ERROR-CHORD] Could not exchange messages");
            //e.printStackTrace();
        }
    }

    public void notify(ChordNodeReference successor) {
        try {
            NotifyMessage request = new NotifyMessage(self);

            //System.out.println("NOTIFYING " + successor.getGuid());
            //System.out.println("Client wrote: " + request);
            this.sendClientMessage(successor.getSocketAddress(), request);
        } catch (Exception e) {
            System.out.println("[ERROR-CHORD] Could not send notify to Peer");
            e.printStackTrace();
        }
    }

    public void fixFingers() {
        //System.out.println("\n\n[CHORD-PERIODIC] fixing fingers...");

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
            //System.out.println("\n\n[CHORD-PERIODIC] checking predecessor...");

            try {
                this.sendAndReceiveMessage(this.predecessor.getSocketAddress(), new CheckMessage(this.getSelfReference()), 2000);
            } catch (Exception e) {
                System.err.println("Couldn't connect to predecessor");
                this.predecessor = null;
            }
        }
    }

    public ChordNodeReference closestPrecedingNode(int id) {
        for (int i = Utils.CHORD_M; i >= 1; i--) {
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

    public NodeStorage getNodeStorage() {
        return nodeStorage;
    }

    public String chordState() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Node id: ").append(this.self.getGuid()).append("\n");
        stringBuilder.append("\nPredecessor: ").append(this.predecessor).append("\n");
        stringBuilder.append("\nRouting Table:\n");
        for (int i = 0; i < this.routingTable.length; i++) {
            stringBuilder.append(i).append("-").append(this.routingTable[i]).append("\n");
        }
        stringBuilder.append("\nSuccessor Table:\n");
        for (int i = 0; i < this.successorsList.length; i++) {
            stringBuilder.append(i).append("-").append(this.successorsList[i]).append("\n");
        }

        return stringBuilder.toString();
    }

    public void shutdownNode() {
        this.scheduler.shutdown();
        System.out.println("[CHORD] Node shutdown successfully");
    }
}
