package chord;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

class PeerReference{

    private int id;
    private InetSocketAddress address;

    public PeerReference(int id, InetSocketAddress address) {
        this.id = id;
        this.address = address;
    }

}

public class ReferenceTable {
    
    ConcurrentHashMap<Integer, PeerReference> routingTable;

    private PeerReference successor;
    private PeerReference predecessor;
    private PeerReference bootPeer;

    public ReferenceTable(int bootId, InetSocketAddress bootAddress){
        this.bootPeer = new PeerReference(bootId, bootAddress);
    }

    public void setPredecessor(int predecessorId, InetSocketAddress predecessorAddress) {
        this.predecessor = new PeerReference(predecessorId,predecessorAddress);
    }

    public void setSuccessor(int successorId, InetSocketAddress successorAddress) {
        this.successor = new PeerReference(successorId,successorAddress);
    }

    public PeerReference getBootPeer(){
        return bootPeer;
    }
}
