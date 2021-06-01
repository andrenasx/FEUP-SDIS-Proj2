package tasks.chord;

import chord.ChordNodeReference;
import messages.chord.GetPredecessorMessage;
import messages.chord.PredecessorMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class GetPredecessorTask extends Task {
    public GetPredecessorTask(GetPredecessorMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        //sends response with my predecessor
        ChordNodeReference predecessor = this.peer.getPredecessor();

        PredecessorMessage response = new PredecessorMessage(peer.getSelfReference(), predecessor);

        try {
            this.peer.sendMessage(socket, response);
            //System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("[ERROR-CHORD] Couldn't send PREDECESSOREPLY");
            e.printStackTrace();
        }
    }
}
