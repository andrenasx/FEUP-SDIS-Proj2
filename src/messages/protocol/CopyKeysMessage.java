package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;
import tasks.protocol.CopyKeysTask;

import javax.net.ssl.SSLSocket;

public class CopyKeysMessage extends Message {
    public CopyKeysMessage(ChordNodeReference senderReference) {
        super("COPY", senderReference);
    }

    @Override
    public CopyKeysTask getTask(Peer peer, SSLSocket socket) {
        return new CopyKeysTask(this, peer, socket);
    }
}
