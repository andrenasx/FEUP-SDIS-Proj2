package tasks.chord;

import messages.chord.SuccessorsMessage;
import messages.chord.SuccessorsReplyMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.Arrays;

public class SuccessorsTask extends Task {
    public SuccessorsTask(SuccessorsMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        SuccessorsReplyMessage response = new SuccessorsReplyMessage(peer.getSelfReference(), Arrays.copyOfRange(peer.getSuccessorsList(), 0, 2));

        try {
            this.peer.sendMessage(socket, response);
            //System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("[ERROR-CHORD] Couldn't send SUCCESSORSREPLY");
            e.printStackTrace();
        }
    }
}
