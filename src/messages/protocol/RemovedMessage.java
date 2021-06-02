package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;
import tasks.protocol.RemovedTask;

import javax.net.ssl.SSLSocket;

public class RemovedMessage extends Message {
    private String fileId;

    private int removedKey;

    public RemovedMessage(ChordNodeReference senderReference, String fileId, int removedKey) {
        super(senderReference);
        this.fileId = fileId;
        this.removedKey = removedKey;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return new RemovedTask(this, peer, socket);
    }

    public String getFileId() {
        return this.fileId;
    }

    public int getRemovedKey() {
        return this.removedKey;
    }
}

