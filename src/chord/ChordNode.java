package chord;

import messages.chord.*;
import messages.protocol.*;
import peer.Peer;
import storage.NodeStorage;
import storage.StorageFile;
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
    private ChordNodeReference[] successorsList = new ChordNodeReference[Utils.CHORD_R];
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

    public boolean join() {
        InetSocketAddress bootSocketAddress = this.bootPeer.getSocketAddress();

        if (this.boot) {
            this.self.setGuid(generateId(bootSocketAddress));
            this.bootPeer = this.self;

            //boot peer will be its successor
            this.setSuccessor(this.self);
            this.setSuccessorsList(0, this.self);

            System.out.println("[CHORD NODE] Boot node with guid: " + this.self.getGuid());

            // Load storage
            this.nodeStorage = NodeStorage.loadState(this.self.getGuid());
            return true;
        }

        try {
            // Send Message to join ring, receiving our assigned guid and our successor
            JoinMessage joinMessage = new JoinMessage(this.self);
            GuidMessage guidMessage = (GuidMessage) this.sendAndReceiveMessage(bootSocketAddress, joinMessage);

            this.self.setGuid(guidMessage.getNewGuid());
            System.out.println("[CHORD] Node guid " + guidMessage.getNewGuid());

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

            int num_files = copyKeysReplyMessage.getDelegatedFiles().size();
            System.out.println("[COPY] Receiving " + num_files + " delegated files from sucessor " + this.getSuccessor());

            if (num_files > 0) {
                List<Future<Boolean>> files = new ArrayList<>();
                for (StorageFile delegatedFile : copyKeysReplyMessage.getDelegatedFiles()) {
                    files.add(((Peer) this).getScheduler().submit(() -> storeDelegatedFile(delegatedFile, this.getSuccessor())));
                }

                for (Future<Boolean> result : files) {
                    if (!result.get()) {
                        System.err.println("[ERROR-CHORD] Couldn't store delegated file");
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR-CHORD] Could not get delegated files");
            return false;
        }

        return true;
    }

    public boolean storeDelegatedFile(StorageFile delegatedFile, ChordNodeReference targetNode) {
        GetFileMessage getFileMessage = new GetFileMessage(this.getSelfReference(), delegatedFile.getFileId());
        if (!((Peer) this).restoreFile(getFileMessage, targetNode, this.getNodeStorage().getStoragePath() + delegatedFile.getFileId())) {
            System.err.println("[ERROR-COPY] Couldn't restore " + delegatedFile.getFilePath());
        }

        System.out.println("[COPY] Successfully stored delegated file " + delegatedFile.getFilePath());

        DeleteMessage deleteMessage = new DeleteMessage(targetNode, delegatedFile.getFileId());
        ((Peer) this).deleteFile(deleteMessage, targetNode, delegatedFile, true);

        this.nodeStorage.addStoredFile(delegatedFile);
        return true;
    }

    public ChordNodeReference findSuccessor(int guid) {
        // In case there's only one peer in the network return self
        if (this.getSuccessor().getGuid() == self.getGuid()) {
            return self;
        }

        // If guid is between our guid and successor guid return successor
        if (this.between(guid, self.getGuid(), this.getSuccessor().getGuid(), true)) {
            return this.getSuccessor();
        }

        // Else send a lookup to the closest preceding node of given guid to find the desired node
        ChordNodeReference closest = this.closestPrecedingNode(guid);
        try {
            LookupMessage request = new LookupMessage(self, guid);
            LookupReplyMessage response = (LookupReplyMessage) this.sendAndReceiveMessage(closest.getSocketAddress(), request);

            return response.getNode();
        } catch (Exception e) {
            System.out.println("[ERROR-CHORD] Could not exchange messages with " + closest);
            return self;
        }
    }

    public void stabilize() {
        this.setSuccessorsList(0, getSuccessor());

        boolean success = false;
        int nextSuccessor = 0;
        while (!success) {
            try {
                SuccessorsMessage request = new SuccessorsMessage(self);
                SuccessorsReplyMessage response = (SuccessorsReplyMessage) this.sendAndReceiveMessage(getSuccessor().getSocketAddress(), request);

                System.arraycopy(response.getSuccessors(), 0, this.successorsList, 1, 2);
                success = true;

            } catch (Exception e) {
                System.out.println("[ERROR-CHORD] Successor down " + getSuccessor());
                nextSuccessor++;
                if (nextSuccessor == 3) return;
                setSuccessor(this.successorsList[nextSuccessor]);
                this.setSuccessorsList(0, getSuccessor());
            }
        }

        try {
            GetPredecessorMessage request = new GetPredecessorMessage(self);
            PredecessorMessage response = (PredecessorMessage) this.sendAndReceiveMessage(getSuccessor().getSocketAddress(), request);

            ChordNodeReference predecessor = response.getPredecessor();
            if (predecessor != null && this.between(predecessor.getGuid(), self.getGuid(), getSuccessor().getGuid(), false)) {
                this.setSuccessor(predecessor);
            }

            if (this.getSuccessor().getGuid() != this.self.getGuid())
                notify(getSuccessor());

        } catch (Exception e) {
            System.out.println("[ERROR-CHORD] Could not exchange messages with" + getSuccessor());
        }
    }

    public void notify(ChordNodeReference successor) {
        try {
            NotifyMessage request = new NotifyMessage(self);

            this.sendClientMessage(successor.getSocketAddress(), request);
        } catch (Exception e) {
            System.out.println("[ERROR-CHORD] Could not send notify to successor " + successor);
        }
    }

    public void fixFingers() {
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
            try {
                this.sendAndReceiveMessage(this.predecessor.getSocketAddress(), new CheckMessage(this.getSelfReference()), 2000);
            } catch (Exception e) {
                System.err.println("[ERROR-CHORD] Could not connect to predecessor " + this.predecessor);
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

    public ChordNodeReference getSelfReference() {
        return this.self;
    }

    public ChordNodeReference getBootReference() {
        return this.bootPeer;
    }

    public NodeStorage getNodeStorage() {
        return nodeStorage;
    }

    public synchronized void setSuccessorsList(ChordNodeReference[] successorsList) {
        this.successorsList = successorsList;
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
        super.stop();
        System.out.println("[CHORD] Node shutdown successfully");
    }

    public void shutdownSafely() {
        if (this.getPredecessor() != null) {
            try {
                AlertPredecessorMessage alertPredecessorMessage = new AlertPredecessorMessage(this.self, this.getSuccessorsList());
                this.sendClientMessage(this.getPredecessor().getSocketAddress(), alertPredecessorMessage);
            } catch (Exception e) {
                System.out.println("[CHORD] Failed sending alert message to predecessor " + this.getPredecessor());
                //e.printStackTrace();
            }
        }

        if (this.getSuccessor().getGuid() != this.getSelfReference().getGuid()) {
            try {
                AlertSuccessorMessage alertSuccessorMessage = new AlertSuccessorMessage(this.self, new ArrayList<>(this.getNodeStorage().getStoredFiles().values()), this.predecessor);
                this.sendAndReceiveMessage(this.getSuccessor().getSocketAddress(), alertSuccessorMessage);
            } catch (Exception e) {
                System.out.println("[CHORD] Failed sending alert message to successor " + this.getSuccessor());
                //e.printStackTrace();
            }
        }
    }
}
