package chord;

import messages.ChordMessage;
import messages.GuidMessage;
import messages.JoinMessage;
import sslengine.SSLEngineClient;
import sslengine.SSLEngineServer;
import utils.Utils;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;

import static utils.Utils.generateId;

public class ChordNode extends SSLEngineServer {
    private SSLContext context;

    private boolean boot;
    private ChordNodeReference self;
    private ChordNodeReference bootPeer;
    private ChordNodeReference predecessor;
    private ChordNodeReference[] routingTable = new ChordNodeReference[Utils.CHORD_M];

    public ChordNode(InetSocketAddress socketAddress, InetSocketAddress bootSocketAddress, SSLContext context, boolean boot) throws Exception {
        super(context, socketAddress);
        this.boot = boot;

        this.context = context;

        this.self = new ChordNodeReference(this.getSocketAddress(), -1);
        this.bootPeer = new ChordNodeReference(bootSocketAddress, -1);

        new Thread(() -> {
            try {
                this.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("My Address " + this.getSocketAddress());
        System.out.println("Boot Address " + bootPeer.getSocketAddress());

        this.joinRing();
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

    public void joinRing() {
        InetSocketAddress bootSocketAddress = bootPeer.getSocketAddress();

        if(this.boot){
            this.self.setGuid(generateId(bootSocketAddress));

            //boot peer will be its successor
            this.setChordNodeReference(1, new ChordNodeReference(bootSocketAddress, this.self.getGuid()));
            System.out.println("Peer started as boot with id: " + this.self.getGuid());
            return;
        }

        try {
            System.out.println("Joining " + this.bootPeer.getSocketAddress());
            SSLEngineClient client = new SSLEngineClient(this.context, bootSocketAddress);
            client.connect();

            JoinMessage request = new JoinMessage(this.self);
            client.write(request.encode());
            System.out.println("Client sent: " + request);

            ChordMessage response = ChordMessage.create(client.read());
            System.out.println("Client received: " + response);
            response.getTask(this, null, null).run();

            client.shutdown();
        } catch(Exception e) {
            System.out.println("Could not connect to peer");
            e.printStackTrace();
        }
    }

    /*public ChordNodeReference findSuccessor(int id){
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
        return new ChordNodeReference(this.self.getSocketAddress(), self.getId());
    }*/

    public boolean between(int id, int currentId, int successorId, boolean includeSuccessor) {
        if(includeSuccessor)
            return id > currentId && id <= successorId;
        else
            return id > currentId && id < successorId;
    }

    public ChordNodeReference getSelfReference () {
        return this.self;
    }

    public ChordNodeReference getBootReference () {
        return this.bootPeer;
    }
}
