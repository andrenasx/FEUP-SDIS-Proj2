package chord;

import messages.*;
import sslengine.SSLEngineClient;
import sslengine.SSLEngineServer;
import utils.Utils;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static utils.Utils.generateId;

public class ChordNode extends SSLEngineServer {
    private SSLContext context;

    private boolean boot;
    private ChordNodeReference self;
    private ChordNodeReference bootPeer;
    private ChordNodeReference predecessor;
    private ChordNodeReference[] routingTable = new ChordNodeReference[Utils.CHORD_M];
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);
    private int next = 0; // it will be incremented to 1 at fixfingers

    public ChordNode(InetSocketAddress socketAddress, InetSocketAddress bootSocketAddress, SSLContext context, boolean boot) throws Exception {
        super(context, socketAddress);
        this.boot = boot;

        this.context = context;

        this.self = new ChordNodeReference(this.getSocketAddress(), -1);
        this.bootPeer = new ChordNodeReference(bootSocketAddress, -1);

        System.out.println("My Address " + this.getSocketAddress());
        System.out.println("Boot Address " + bootPeer.getSocketAddress());
    }

    protected void startPeriodicStabilize(){
        scheduler.scheduleAtFixedRate(this::stabilize,10, 10, TimeUnit.SECONDS);
        //scheduler.scheduleAtFixedRate(this::fixFingers,10, 10, TimeUnit.SECONDS);
        //scheduler.scheduleAtFixedRate(this::checkPredecessor,10, 14, TimeUnit.SECONDS);
    }

    public synchronized ChordNodeReference predecessor() { return predecessor; }

    public synchronized void setPredecessor(ChordNodeReference predecessor) { this.predecessor = predecessor; }

    public synchronized ChordNodeReference successor() {
        return routingTable[0];
    }

    public synchronized ChordNodeReference getRoutingTable(int position){
        return routingTable[position - 1];
    }

    public synchronized void setChordNodeReference(int position, ChordNodeReference reference) {
        this.routingTable[position] = reference;
    }

    public void join() {
        InetSocketAddress bootSocketAddress = bootPeer.getSocketAddress();

        if(this.boot){
            this.self.setGuid(generateId(bootSocketAddress));

            //boot peer will be its successor
            this.setChordNodeReference(0, new ChordNodeReference(bootSocketAddress, this.self.getGuid()));
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

    public ChordNodeReference findSuccessor(int guid){
        System.out.println("\n\nfinding successor...");
        System.out.println(successor());

        if (successor().getGuid() == self.getGuid()) { //in case there's only one peer in the network
            System.out.println("returned self");
            return self;
        }

        if(between(guid, self.getGuid(), successor().getGuid(), true)) {
            System.out.println("returned successor");
            return successor();
        }

        ChordNodeReference closest = closestPrecedingNode(guid);
        System.out.println("Closest to " + guid + ": " + closest);

        try{
            SSLEngineClient client = new SSLEngineClient(this.context, closest.getSocketAddress());
            client.connect();

            ChordMessage request = new LookupMessage(self, String.valueOf(guid).getBytes(StandardCharsets.UTF_8));

            //Send lookup
            client.write(request.encode());
            System.out.println("Client sent: " + request);

            //receive response
            ChordMessage response = ChordMessage.create(client.read());
            System.out.println("Client received: " + response);

            client.shutdown();

            return ((LookupReplyMessage) response).getSuccessor();

        }catch(Exception e){
            System.out.println("Could not connect to peer");
            e.printStackTrace();
            return null;
        }
    }

    public void stabilize(){
        System.out.println("\n\nstabilizing...");

        try {
            SSLEngineClient client = new SSLEngineClient(this.context, successor().getSocketAddress());
            client.connect();

            PredecessorMessage request = new PredecessorMessage(self, null);

            //Send predecessor request
            client.write(request.encode());
            System.out.println("Client sent: " + request);

            //receive predecessor response
            ChordMessage response = ChordMessage.create(client.read());
            System.out.println("Client received: " + response);

            client.shutdown();

            if(response instanceof ErrorMessage) { return; } //server could not handle our request, aborts stabilize

            ChordNodeReference x = ((PredecessorReplyMessage) response).getPredecessor();

            if (x != null && between(x.getGuid(),self.getGuid(),successor().getGuid(),false)){
                //set node successor
                setChordNodeReference(0,x);
            }

            notify(successor());

        }catch(Exception e){
            System.out.println("Could not connect to Peer");
            e.printStackTrace();
        }

    }

    public void notify(ChordNodeReference successor){
        System.out.println(" RUNNING NOTIFY FUNCTION");
        try {
            SSLEngineClient client = new SSLEngineClient(this.context, successor.getSocketAddress());
            client.connect();

            NotifyMessage request = new NotifyMessage(self);

            //Send notify
            client.write(request.encode());
            System.out.println("Client sent: " + request);

            client.shutdown();
        }catch (Exception e){
            System.out.println("Could not connect to Peer");
            e.printStackTrace();
        }
    }

    public void fixFingers(){
        System.out.println("\n\nfixing fingers...");
        next = next + 1;
        if(next >= Utils.CHORD_M)
            next = 1;
        System.out.println("Current next: " + next);
        this.setChordNodeReference(next, this.findSuccessor(this.self.getGuid() + 2^(next-1)));
    }

    public void checkPredecessor(){
        System.out.println("\n\nchecking predecessor...");
        try {
            if(predecessor == null) { return; }
            SSLEngineClient client = new SSLEngineClient(this.context, predecessor.getSocketAddress());
            client.connect();

            AliveMessage request = new AliveMessage(self);

            //Send notify
            client.write(request.encode());
            System.out.println("Client sent: " + request);

            //receive response
            ChordMessage response = ChordMessage.create(client.read());
            System.out.println("Client received: " + response);

            client.shutdown();
        }catch (Exception e){
            System.out.println("Could not connect to Peer");
            e.printStackTrace();
        }
    }

    public ChordNodeReference closestPrecedingNode(int id){
        for(int i = routingTable.length-1; i >= 0; i--){
            if(routingTable[i] != null && between(routingTable[i].getGuid(), self.getGuid(), id, false))
                return routingTable[i];
        }
        return self;
    }

    public boolean between(int id, int currentId, int successorId, boolean includeSuccessor) {
        if(includeSuccessor)
            return currentId < successorId ? (currentId < id && id <= successorId) : (successorId >= id || id > currentId);
        else
            return currentId < successorId ? (currentId < id && id < successorId) : (successorId > id || id > currentId);
    }

    public ChordNodeReference getSelfReference () {
        return this.self;
    }

    public ChordNodeReference getBootReference () {
        return this.bootPeer;
    }

    public SSLContext getContext(){
        return this.context;
    }

    public String chordState(){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Node id: ").append(this.self.getGuid()).append("\n");
        stringBuilder.append("\nPredecessor: ").append(this.predecessor).append("\n");
        stringBuilder.append("\nRouting table:\n").append("\n");
        for (int i = 0; i < routingTable.length; i++){
            stringBuilder.append(i).append("-").append(routingTable[i]).append("\n");
        }

        return stringBuilder.toString();
    }
}
