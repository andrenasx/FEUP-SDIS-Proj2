package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class FileMessage extends Message {
    private final byte[] fileData;

    public FileMessage(ChordNodeReference senderReference, byte[] fileData) {
        super(senderReference);
        this.fileData = fileData;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    public byte[] getFileData() {
        return this.fileData;
    }
}
