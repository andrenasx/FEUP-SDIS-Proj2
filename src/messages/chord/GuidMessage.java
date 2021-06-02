package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class GuidMessage extends Message {
    private final int guid;
    private final ChordNodeReference successor;

    public GuidMessage(ChordNodeReference senderReference, int guid, ChordNodeReference successor) {
        super(senderReference);

        this.guid = guid;
        this.successor = successor;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    @Override
    public String toString() {
        return "GuidMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", newGuid=" + this.guid +
                ", successor=" + this.successor +
                '}';
    }

    public int getNewGuid() {
        return this.guid;
    }

    public ChordNodeReference getSuccessorReference() {
        return this.successor;
    }
}
