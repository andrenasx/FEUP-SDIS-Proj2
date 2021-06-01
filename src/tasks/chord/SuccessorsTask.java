package tasks.chord;

import chord.ChordNodeReference;
import messages.chord.SuccessorsMessage;
import messages.chord.SuccessorsReplyMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.*;

public class SuccessorsTask extends Task {
    public SuccessorsTask(SuccessorsMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        //Create set from array elements
        LinkedHashSet<ChordNodeReference> linkedHashSet = new LinkedHashSet<>(Arrays.asList(peer.getRoutingTable()));
        //Get back the array without duplicates
        ChordNodeReference[] successors = linkedHashSet.toArray(new ChordNodeReference[] {});

        ChordNodeReference[] toSend = null;

        if(successors.length > 2)
            toSend = Arrays.copyOfRange(successors, 0, 3);
        else
            toSend = Arrays.copyOfRange(successors, 0, successors.length);

        SuccessorsReplyMessage response = new SuccessorsReplyMessage(peer.getSelfReference(), toSend);

        try {
            this.peer.sendMessage(socket, response);
            //System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("[ERROR-CHORD] Couldn't send SUCCESSORSREPLY");
            e.printStackTrace();
        }
    }
}
