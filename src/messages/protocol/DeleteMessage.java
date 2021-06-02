package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.protocol.DeleteTask;

import javax.net.ssl.SSLSocket;

public class DeleteMessage extends Message {
    private final String fileId;

    public DeleteMessage(ChordNodeReference senderReference, String fileId) {
        super(senderReference);

        this.fileId = fileId;
    }

    @Override
    public DeleteTask getTask(Peer peer, SSLSocket socket) {
        return new DeleteTask(this, peer, socket);
    }

    public String getFileId() {
        return fileId;
    }
}
