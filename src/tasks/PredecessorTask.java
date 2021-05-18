package tasks;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.LookupMessage;
import messages.LookupReplyMessage;
import messages.PredecessorMessage;
import messages.PredecessorReplyMessage;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class PredecessorTask extends ChordTask {
    public PredecessorTask(PredecessorMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, channel, engine);
    }

    @Override
    public void run() {
        //sends response with my predecessor
        ChordNodeReference predecessor = this.node.predecessor();

        String predecessorStr = predecessor != null ? this.node.predecessor().toString() : "";

        PredecessorReplyMessage response = new PredecessorReplyMessage(node.getSelfReference(), predecessorStr.getBytes(StandardCharsets.UTF_8));

        try {
            node.write(channel, engine, response.encode());
            System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("Couldn't send PREDECESSOREPLY");
            e.printStackTrace();
        }
    }
}
