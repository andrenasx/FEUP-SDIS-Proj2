package tasks.protocol;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.chord.LookupMessage;
import messages.chord.LookupReplyMessage;
import tasks.Task;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class LookupTask extends Task {
    public LookupTask(LookupMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, channel, engine);
    }

    @Override
    public void run() {
        //sets guid
        int requestedId = ((LookupMessage) this.message).getRequestedGuid();
        //System.out.println("LookupTask searching for successor of " + requestedId);

        //send find successor after receiving guid
        ChordNodeReference reference = node.findSuccessor(requestedId);

        LookupReplyMessage response = new LookupReplyMessage(node.getSelfReference(), reference.toString().getBytes(StandardCharsets.UTF_8));

        try {
            node.write(channel, engine, response.encode());
            //System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("[ERROR-CHORD] Couldn't send LOOKUP");
            e.printStackTrace();
        }
    }
}
