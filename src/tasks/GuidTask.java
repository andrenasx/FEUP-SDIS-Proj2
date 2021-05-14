package tasks;

import chord.ChordNode;
import messages.GuidMessage;

public class GuidTask extends ChordTask {
    public GuidTask(GuidMessage message, ChordNode node) {
        super(message, node, null, null);
    }

    @Override
    public void run() {
        this.node.getSelfReference().setGuid(((GuidMessage) this.message).getNewGuid());
        System.out.println("Peer with id " + this.node.getSelfReference().getGuid());
    }
}
