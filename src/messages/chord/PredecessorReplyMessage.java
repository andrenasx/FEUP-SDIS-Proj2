package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

//CHORD PREDECESSORREPLY <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <PRED. GUID> <PRED. IP> <PRED. PORT>
public class PredecessorReplyMessage extends Message {
    private final ChordNodeReference predecessor;

    public PredecessorReplyMessage(ChordNodeReference senderReference, ChordNodeReference predecessor) {
        super("PREDECESSORREPLY", senderReference);
        this.predecessor = predecessor;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    @Override
    public String toString() {
        return "PredecessorReplyMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", predecessor=" + this.predecessor +
                '}';
    }

    public ChordNodeReference getPredecessor() {
        return this.predecessor;
    }
}
