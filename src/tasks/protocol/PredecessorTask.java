package tasks.protocol;

import chord.ChordNodeReference;
import messages.chord.PredecessorMessage;
import messages.chord.PredecessorReplyMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class PredecessorTask extends Task {
    public PredecessorTask(PredecessorMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        //sends response with my predecessor
        ChordNodeReference predecessor = this.peer.getPredecessor();

        PredecessorReplyMessage response = new PredecessorReplyMessage(peer.getSelfReference(), predecessor);

        try {
            this.peer.sendMessage(socket, response);
            //System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("[ERROR-CHORD] Couldn't send PREDECESSOREPLY");
            e.printStackTrace();
        }
    }
}
