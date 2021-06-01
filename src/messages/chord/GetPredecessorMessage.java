package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.chord.GetPredecessorTask;

import javax.net.ssl.SSLSocket;

//CHORD PREDECESSOR <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n
public class GetPredecessorMessage extends Message {
    public GetPredecessorMessage(ChordNodeReference senderReference) {
        super(senderReference);
    }

    @Override
    public GetPredecessorTask getTask(Peer peer, SSLSocket socket) {
        return new GetPredecessorTask(this, peer, socket);
    }

    @Override
    public String toString() {
        return "GetPredecessorMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
