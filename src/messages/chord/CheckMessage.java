package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.chord.CheckTask;

import javax.net.ssl.SSLSocket;

public class CheckMessage extends Message {
    public CheckMessage(ChordNodeReference senderReference) {
        super(senderReference);
    }

    @Override
    public CheckTask getTask(Peer peer, SSLSocket socket) {
        return new CheckTask(this, peer, socket);
    }
}
