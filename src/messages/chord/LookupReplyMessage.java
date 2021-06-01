package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

//CHORD LOOKUPREPLY <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <CLOSEST PRED. GUID> <CLOSEST PRED. IP> <CLOSEST PRED. PORT>
public class LookupReplyMessage extends Message {
    private final ChordNodeReference node;

    public LookupReplyMessage(ChordNodeReference senderReference, ChordNodeReference successor) {
        super(senderReference);
        this.node = successor;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    @Override
    public String toString() {
        return "LookupReplyMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", successor=" + this.node +
                '}';
    }

    public ChordNodeReference getNode() {
        return this.node;
    }
}
