package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.protocol.RestoreTask;

import javax.net.ssl.SSLSocket;

public class RestoreMessage extends Message {
    private final String fileId;

    public RestoreMessage(ChordNodeReference senderReference, String fileId) {
        super("REMOVE", senderReference);
        this.fileId = fileId;
    }

    @Override
    public RestoreTask getTask(Peer peer, SSLSocket socket) {
        return new RestoreTask(this, peer, socket);
    }

    public String getFileId() {
        return this.fileId;
    }
}
