package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class PredecessorMessage extends Message {
    private final ChordNodeReference predecessor;

    public PredecessorMessage(ChordNodeReference senderReference, ChordNodeReference predecessor) {
        super(senderReference);
        this.predecessor = predecessor;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    @Override
    public String toString() {
        return "PredecessorMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", predecessor=" + this.predecessor +
                '}';
    }

    public ChordNodeReference getPredecessor() {
        return this.predecessor;
    }
}
