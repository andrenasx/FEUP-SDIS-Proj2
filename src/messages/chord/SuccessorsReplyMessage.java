package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.util.Arrays;

//CHORD PREDECESSORREPLY <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <PRED. GUID> <PRED. IP> <PRED. PORT>
public class SuccessorsReplyMessage extends Message {
    private final ChordNodeReference[] successors;

    public SuccessorsReplyMessage(ChordNodeReference senderReference, ChordNodeReference[] successors) {
        super(senderReference);
        this.successors = successors;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    @Override
    public String toString() {
        return "SuccessorsReplyMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", successors=" + Arrays.toString(this.successors) +
                '}';
    }

    public ChordNodeReference[] getSuccessors() {
        return this.successors;
    }
}
