package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.protocol.LookupTask;

import javax.net.ssl.SSLSocket;

//CHORD LOOKUP <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <GUID>
public class LookupMessage extends Message {
    private final int guid;

    public LookupMessage(ChordNodeReference senderReference, int guid) {
        super("LOOKUP", senderReference);
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
