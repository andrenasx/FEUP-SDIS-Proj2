package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.protocol.GetFileTask;

import javax.net.ssl.SSLSocket;

public class GetFileMessage extends Message {
    private final String fileId;

    public GetFileMessage(ChordNodeReference senderReference, String fileId) {
        super("GET", senderReference);
        this.fileId = fileId;
    }

    @Override
    public GetFileTask getTask(Peer peer, SSLSocket socket) {
        return new GetFileTask(this, peer, socket);
    }

    public String getFileId() {
        return this.fileId;
    }
}
