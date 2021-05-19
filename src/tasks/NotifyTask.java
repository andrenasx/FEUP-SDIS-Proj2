package tasks;

import chord.ChordNode;
import messages.GuidMessage;
import messages.JoinMessage;
import messages.NotifyMessage;
import utils.Utils;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NotifyTask extends ChordTask {
    public NotifyTask(NotifyMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, null, null);
    }

    @Override
    public void run() {
        if(this.node.predecessor() == null || this.node.between(this.message.getSenderGuid(), this.node.predecessor().getGuid(), this.node.getSelfReference().getGuid(),false)){
            this.node.setPredecessor(this.message.getSenderNodeReference());
        }
    }
}
