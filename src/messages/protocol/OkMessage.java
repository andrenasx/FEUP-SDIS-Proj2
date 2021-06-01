package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class OkMessage extends Message {
    private String body = null;

    public OkMessage(ChordNodeReference senderReference) {
        super(senderReference);
    }

    public OkMessage(ChordNodeReference senderReference, String body) {
        super(senderReference);
        this.body = body;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    public String getBody() {
        return this.body;
    }
}
