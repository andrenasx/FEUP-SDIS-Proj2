package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class RestoreMessage extends Message {
    private final String fileId;

    public RestoreMessage(ChordNodeReference senderReference, String fileId) {
        super("REMOVE", senderReference);
        this.fileId = fileId;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    public String getFileId() {
        return this.fileId;
    }
}
