package tasks.chord;

import chord.ChordNodeReference;
import messages.chord.LookupMessage;
import messages.chord.LookupReplyMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class LookupTask extends Task {
    public LookupTask(LookupMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        // Sets guid
        int requestedId = ((LookupMessage) this.message).getRequestedGuid();

        // Send find successor after receiving guid
        ChordNodeReference sucessor = peer.findSuccessor(requestedId);

        LookupReplyMessage response = new LookupReplyMessage(peer.getSelfReference(), sucessor);

        try {
            peer.sendMessage(socket, response);
            //System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("[ERROR-CHORD] Couldn't send LOOKUP");
            e.printStackTrace();
        }
    }
}
