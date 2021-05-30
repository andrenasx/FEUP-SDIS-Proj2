package tasks.protocol;

import chord.ChordNode;
import messages.chord.NotifyMessage;
import tasks.Task;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;

public class NotifyTask extends Task {
    public NotifyTask(NotifyMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, channel, engine);
    }

    @Override
    public void run() {
        if (this.node.getPredecessor() == null || this.node.between(this.message.getSenderGuid(), this.node.getPredecessor().getGuid(), this.node.getSelfReference().getGuid(), false)) {
            //System.out.println("SETTING PREDECESSOR " + this.message.getSenderGuid() + " FOR NODE " + this.node.getSelfReference().getGuid());
            this.node.setPredecessor(this.message.getSenderNodeReference());
        }

        this.node.closeConnectionServer(channel, engine);
    }
}
