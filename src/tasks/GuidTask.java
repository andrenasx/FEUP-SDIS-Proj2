package tasks;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.ChordMessage;
import messages.GuidMessage;
import messages.LookupMessage;
import sslengine.SSLEngineClient;
import utils.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class GuidTask extends ChordTask {
    public GuidTask(GuidMessage message, ChordNode node) {
        super(message, node, null, null);
    }

    @Override
    public void run() {
        int newGuid = ((GuidMessage) this.message).getNewGuid();
        this.node.getSelfReference().setGuid(newGuid);
        System.out.println("Peer with id " + this.node.getSelfReference().getGuid());

        //sends lookup for findSuccessor
        try {
            SSLEngineClient client = new SSLEngineClient(this.node.getContext(), this.message.getSenderSocketAddress());
            client.connect();

            LookupMessage request = new LookupMessage(this.node.getSelfReference(), String.valueOf(newGuid).getBytes(StandardCharsets.UTF_8));

            //Send lookup
            client.write(request.encode());
            System.out.println("Client sent: " + request);

            //receive response
            ChordMessage response = ChordMessage.create(client.read());
            System.out.println("Client received: " + response);
            //response.getTask(this.node, null, null).run();

            //updates successor
            this.node.setChordNodeReference(1, new ChordNodeReference(response.getBody()));

            client.shutdown();
        }catch(Exception e){
            System.out.println("Could not connect to peer");
            e.printStackTrace();
        }
    }
}
