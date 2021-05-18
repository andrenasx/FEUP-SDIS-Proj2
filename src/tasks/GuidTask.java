package tasks;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.ChordMessage;
import messages.GuidMessage;
import messages.LookupMessage;
import sslengine.SSLEngineClient;

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

        //updates successor
        this.node.setChordNodeReference(1, new ChordNodeReference(message.getBody()));

    }
}
