package tasks.chord;

import chord.ChordNodeReference;
import messages.chord.AlertPredecessorMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.util.Arrays;

public class AlertPredecessorTask extends Task {
    public AlertPredecessorTask(AlertPredecessorMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        ChordNodeReference[] successorsList = ((AlertPredecessorMessage) message).getSuccessorsList();
        this.peer.setSuccessorsList(successorsList);
        this.peer.setSuccessor(successorsList[0]);
    }
}
