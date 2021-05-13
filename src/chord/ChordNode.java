package chord;

import channel.Server;
import messages.chord.ChordMessage;
import messages.chord.JoinMessage;
import messages.chord.LookupMessage;
import utils.Utils;

import javax.net.ssl.SSLSocket;
import java.net.InetSocketAddress;

import static utils.Utils.generateId;

public class ChordNode extends Server{
    protected boolean boot;
    protected ChordNodeReference self;
    protected ChordNodeReference bootPeer;
    protected ChordNodeReference predecessor;
    protected ChordNodeReference[] routingTable = new ChordNodeReference[Utils.CHORD_M];

    public ChordNode(InetSocketAddress address, boolean boot){
        super(address,boot);
        this.boot = boot;
        this.bootPeer = new ChordNodeReference(address, -1);
        this.self = new ChordNodeReference(this.address,-1);

        // Create Peer Internal State
        //this.storage = PeerStorage.loadState(this);
    }

    public int getNodeId(){
        return  self.getId();
    }

    public void setNodeId(int id){
        self.setId(id);
    }

    public ChordNodeReference successor() {
        return routingTable[0];
    }

    public ChordNodeReference getRoutingTable(int position){
        return routingTable[position - 1];
    }

    public void setChordNodeReference(int position, ChordNodeReference reference) {
        this.routingTable[position - 1] = reference;
    }

    public void joinRing(){
        InetSocketAddress socketAddress = bootPeer.getSocketAddress();

        if(this.boot){
            self.setId(generateId(socketAddress));

            //boot peer will be its successor
            this.setChordNodeReference(1,new ChordNodeReference(socketAddress, self.getId()));
            System.out.println("Peer started as boot with id: " + self.getId());
            return;
        }

        try{
            SSLSocket socket = connect(socketAddress);
            ChordMessage message = new JoinMessage(self);

            //Send join
            this.send_message(message, socket);

            //Receive response and handle task
            this.receive_message(socket).submitTask((Peer) this);

        }catch(Exception e){
            System.out.println("Could not connect to peer");
            e.printStackTrace();
        }
    }

    public ChordNodeReference findSuccessor(int id){
        System.out.println("finding successor");

        if (successor() == null)
            return self;

        if(between(id, self.getId(), successor().getId(), true))
            return successor();

        ChordNodeReference closest = closestPrecedingNode(id);

        try{
            SSLSocket socket = connect(closest.getSocketAddress());
            ChordMessage message = new LookupMessage(self, id);
            //Send lookup
            this.send_message(message, socket);

            //Receive response
            LookupMessage reply = (LookupMessage) this.receive_message(socket);

            return reply.getNodeReference();

        }catch(Exception e){
            System.out.println("Could not connect to peer");
            e.printStackTrace();
            return null;
        }
    }

    public ChordNodeReference closestPrecedingNode(int id){
        for(int i = routingTable.length-1; i >= 0; i--){
            if(between(routingTable[i].getId(), self.getId(), id, false))
                return routingTable[i];
        }
        return new ChordNodeReference(this.address, self.getId());
    }

    public boolean between(int id, int currentId, int successorId, boolean includeSuccessor){
        if(includeSuccessor)
            return id > currentId && id <= successorId;
        else
            return id > currentId && id < successorId;
    }
}
