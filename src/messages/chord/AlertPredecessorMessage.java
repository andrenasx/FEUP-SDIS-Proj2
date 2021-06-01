package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.chord.AlertPredecessorTask;

import javax.net.ssl.SSLSocket;

public class AlertPredecessorMessage extends Message {
    ChordNodeReference[] successorsList;

    public AlertPredecessorMessage(ChordNodeReference senderReference, ChordNodeReference[] successorsList) {
        super(senderReference);
        this.successorsList = successorsList;
    }

    @Override
    public AlertPredecessorTask getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    public ChordNodeReference[] getSuccessorsList() {
        return successorsList;
    }
}
