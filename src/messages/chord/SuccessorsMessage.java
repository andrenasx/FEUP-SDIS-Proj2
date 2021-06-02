package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.chord.SuccessorsTask;

import javax.net.ssl.SSLSocket;

public class SuccessorsMessage extends Message {
    public SuccessorsMessage(ChordNodeReference senderReference) {
        super(senderReference);
    }

    @Override
    public SuccessorsTask getTask(Peer peer, SSLSocket socket) {
        return new SuccessorsTask(this, peer, socket);
    }

    @Override
    public String toString() {
        return "SuccessorsMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
