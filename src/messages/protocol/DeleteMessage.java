package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.protocol.BackupTask;
import tasks.protocol.DeleteTask;

import javax.net.ssl.SSLSocket;

public class DeleteMessage extends Message {

    private final String fileId;

    public DeleteMessage(ChordNodeReference senderReference, String fileId) {
        super("DELETE", senderReference);

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
