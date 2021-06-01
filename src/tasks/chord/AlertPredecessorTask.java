package tasks.chord;

import messages.chord.AlertPredecessorMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class AlertPredecessorTask extends Task {
    public AlertPredecessorTask(AlertPredecessorMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        this.peer.setSuccessorsList(((AlertPredecessorMessage) message).getSuccessorsList());
    }
}
