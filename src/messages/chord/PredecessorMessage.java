package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.protocol.PredecessorTask;

import javax.net.ssl.SSLSocket;

//CHORD PREDECESSOR <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n
public class PredecessorMessage extends Message {
    public PredecessorMessage(ChordNodeReference senderReference) {
        super("PREDECESSOR", senderReference);
    }

    @Override
    public PredecessorTask getTask(Peer peer, SSLSocket socket) {
        return new PredecessorTask(this, peer, socket);
    }

    @Override
    public String toString() {
        return "PredecessorMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
