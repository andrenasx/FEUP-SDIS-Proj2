package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class OkMessage extends Message {
    public OkMessage(ChordNodeReference senderReference) {
        super("OK", senderReference);
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }
}
