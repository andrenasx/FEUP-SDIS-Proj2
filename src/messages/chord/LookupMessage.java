package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.chord.LookupTask;

import javax.net.ssl.SSLSocket;

public class LookupMessage extends Message {
    private final int guid;

    public LookupMessage(ChordNodeReference senderReference, int guid) {
        super(senderReference);
        this.guid = guid;
    }

    @Override
    public LookupTask getTask(Peer peer, SSLSocket socket) {
        return new LookupTask(this, peer, socket);
    }

    @Override
    public String toString() {
        return "LookupMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", guid=" + this.guid +
                '}';
    }

    public int getRequestedGuid() {
        return this.guid;
    }
}
