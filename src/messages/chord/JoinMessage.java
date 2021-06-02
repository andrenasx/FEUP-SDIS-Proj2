package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.chord.JoinTask;

import javax.net.ssl.SSLSocket;

public class JoinMessage extends Message {
    public JoinMessage(ChordNodeReference senderReference) {
        super(senderReference);
    }

    @Override
    public JoinTask getTask(Peer peer, SSLSocket socket) {
        return new JoinTask(this, peer, socket);
    }

    @Override
    public String toString() {
        return "JoinMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
